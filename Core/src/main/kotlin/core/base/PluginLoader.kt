package tw.xinshou.discord.core.base

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.common.Version
import tw.xinshou.discord.core.plugin.PluginEvent
import tw.xinshou.discord.core.plugin.yaml.InfoSerializer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayDeque
import java.util.EnumSet
import java.util.LinkedHashMap
import java.util.jar.JarFile
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

internal object PluginLoader {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val dir: Path = Paths.get("plugins")
    private val pluginInfos: MutableMap<String, InfoSimple> = HashMap()
    private val pluginVersionPattern = Regex("^\\d+\\.\\d+\\.\\d+(?:-[A-Za-z0-9.-]+)?$")

    val intents: EnumSet<GatewayIntent> = EnumSet.noneOf(GatewayIntent::class.java)
    val cacheFlags: EnumSet<CacheFlag> = EnumSet.noneOf(CacheFlag::class.java)
    val memberCachePolicies: MutableSet<String> = mutableSetOf("OWNER", "VOICE")
    val guildCommands = mutableListOf<CommandData>()
    val globalCommands = mutableListOf<CommandData>()
    val listenersQueue = ArrayDeque<PluginEvent>()
    val pluginQueue = LinkedHashMap<String, PluginEvent>()

    fun preLoad() {
        var discovered = 0
        var failed = 0
        logger.info("Start loading plugins...")

        resetState()
        Files.createDirectories(dir)

        dir.listDirectoryEntries()
            .filter { it.extension == "jar" && it.toFile().isFile }
            .also { logger.info("Found {} plugin(s).", it.size) }
            .forEach { path ->
                JarFile(path.toFile()).use { jarFile ->
                    try {
                        val infoEntry = jarFile.getEntry("info.yaml")
                        if (infoEntry == null) {
                            failed++
                            logger.error("Missing info.yaml in plugin jar: {}", path.name)
                            return@forEach
                        }

                        jarFile.getInputStream(infoEntry).use { inputStream ->
                            val config = Yaml().decodeFromStream<InfoSerializer>(inputStream)

                            if (config.name in pluginInfos) {
                                failed++
                                logger.error("Duplicate plugin name: {} ({})", config.name, path.name)
                                return@forEach
                            }

                            if (!validatePluginInfo(path, config)) {
                                failed++
                                return@forEach
                            }

                            val dependJars = resolveDependencyJars(path, config) ?: run {
                                failed++
                                return@forEach
                            }

                            pluginInfos[config.name] = InfoSimple(
                                name = config.name,
                                mainClass = config.main,
                                path = path,
                                depend = config.dependPlugins,
                                softDepend = config.softDependPlugins,
                                dependJars = dependJars,
                                prefix = config.prefix,
                                componentPrefix = config.componentPrefix
                            )

                            intents.addAll(
                                config.requireIntents.mapNotNull { runCatching { GatewayIntent.valueOf(it) }.getOrNull() }
                            )
                            cacheFlags.addAll(
                                config.requireCacheFlags.mapNotNull { runCatching { CacheFlag.valueOf(it) }.getOrNull() }
                            )
                            memberCachePolicies.addAll(config.requireMemberCachePolicies)
                            discovered++
                        }
                    } catch (e: Exception) {
                        failed++
                        logger.error("Error occurred with file: {}!", path.name, e)
                    }
                }
            }

        val ordered = LinkedHashMap<String, InfoSimple>()
        pluginInfos.values.forEach { info ->
            if (!ordered.containsKey(info.name) && addPluginToQueue(info, ordered, mutableSetOf())) {
                failed++
            }
        }

        ordered.values.forEach { info ->
            try {
                tw.xinshou.discord.core.plugin.ClassLoader.createPluginLoader(
                    pluginName = info.name,
                    primaryJar = info.path,
                    dependencyPluginNames = info.depend + info.softDepend.filter(pluginInfos::containsKey),
                    additionalJars = info.dependJars
                )

                val pluginClass = tw.xinshou.discord.core.plugin.ClassLoader.getClass(info.name, info.mainClass)
                    ?: throw ClassNotFoundException(info.mainClass)

                val pluginInstance = pluginClass.getDeclaredField("INSTANCE").get(null) as? PluginEvent
                    ?: throw IllegalStateException("Cannot get object instance of plugin: ${info.name}")

                pluginQueue[info.name] = pluginInstance.apply {
                    pluginName = info.name
                    prefix = info.prefix.ifBlank { info.name }
                    componentPrefix = normalizeComponentPrefix(info.name, info.componentPrefix)
                }

                logger.info("==ADD==> {}", info.name)
            } catch (e: Exception) {
                failed++
                logger.error("Failed to initialize plugin: {}", info.name, e)
            }
        }

        if (failed > 0) logger.error("{} plugin(s) failed to load.", failed)
        logger.info("{} plugin(s) found successfully.", discovered)
    }

    fun run() {
        listenersQueue.clear()
        pluginQueue.values.reversed().forEach { plugin ->
            plugin.load()

            logger.info("{} load successfully.", plugin.pluginName)
            if (plugin.listener) listenersQueue.add(plugin)
        }

        rebuildCommands()
    }

    /**
     * Reloads all plugins by calling their reload methods.
     */
    fun reload() {
        pluginQueue.values.forEach { it.reload() }
        rebuildCommands()
    }

    fun reloadPlugin(pluginName: String) {
        val plugin = pluginQueue[pluginName]
            ?: throw IllegalArgumentException("Plugin '$pluginName' is not loaded.")
        plugin.reload()
        rebuildCommands()
    }

    fun rebuildCommands() {
        guildCommands.clear()
        globalCommands.clear()

        pluginQueue.values.reversed().forEach { plugin ->
            plugin.guildCommands()?.let { guildCommands.addAll(it) }
            plugin.globalCommands()?.let { globalCommands.addAll(it) }
        }
    }

    fun closeClassLoaders() {
        tw.xinshou.discord.core.plugin.ClassLoader.clear()
    }

    private fun addPluginToQueue(
        pluginInfo: InfoSimple?,
        orderedPlugins: LinkedHashMap<String, InfoSimple>,
        visiting: MutableSet<String>,
    ): Boolean {
        pluginInfo ?: return true
        if (pluginInfo.name in orderedPlugins) return false

        if (!visiting.add(pluginInfo.name)) {
            logger.error("Detected circular plugin dependency around '{}'.", pluginInfo.name)
            return true
        }

        pluginInfo.depend.forEach { depend ->
            if (depend !in pluginInfos) {
                logger.error("Plugin '{}' is missing dependency '{}'.", pluginInfo.name, depend)
                return true
            }

            if (addPluginToQueue(pluginInfos[depend], orderedPlugins, visiting)) {
                logger.error("Failed to load dependency '{}' for plugin '{}'.", depend, pluginInfo.name)
                return true
            }
        }

        pluginInfo.softDepend.forEach { depend ->
            if (depend in pluginInfos) {
                addPluginToQueue(pluginInfos[depend], orderedPlugins, visiting)
            }
        }

        visiting.remove(pluginInfo.name)
        orderedPlugins[pluginInfo.name] = pluginInfo
        return false
    }

    private fun validatePluginInfo(path: Path, config: InfoSerializer): Boolean {
        if (config.coreApi != Version.CORE_API) {
            logger.error(
                "Plugin '{}' ({}) is built for coreApi '{}', current coreApi is '{}'.",
                config.name,
                path.name,
                config.coreApi,
                Version.CORE_API
            )
            return false
        }

        if (!pluginVersionPattern.matches(config.version)) {
            logger.error(
                "Plugin '{}' ({}) has invalid version format '{}'.",
                config.name,
                path.name,
                config.version
            )
            return false
        }

        return true
    }

    private fun resolveDependencyJars(path: Path, config: InfoSerializer): Set<Path>? {
        val baseDir = path.parent ?: Paths.get(".")
        val jars = linkedSetOf<Path>()

        config.dependJars.forEach { jarPath ->
            val resolved = resolveJarPath(baseDir, jarPath)
            if (!resolved.isRegularFile()) {
                logger.error("Plugin '{}' missing required dependency jar: {}", config.name, resolved)
                return null
            }
            jars.add(resolved)
        }

        config.softDependJars.forEach { jarPath ->
            val resolved = resolveJarPath(baseDir, jarPath)
            if (resolved.isRegularFile()) {
                jars.add(resolved)
            } else {
                logger.warn("Plugin '{}' optional dependency jar not found: {}", config.name, resolved)
            }
        }

        return jars
    }

    private fun resolveJarPath(baseDir: Path, rawPath: String): Path {
        val path = Paths.get(rawPath)
        return if (path.isAbsolute) path.normalize() else baseDir.resolve(path).normalize()
    }

    private fun normalizeComponentPrefix(pluginName: String, infoComponentPrefix: String): String {
        if (infoComponentPrefix.isNotBlank()) return infoComponentPrefix

        if (pluginName.length > 10) {
            logger.warn("Plugin name '{}' is too long, generated component prefix may exceed limits.", pluginName)
        }

        return pluginName.lowercase() + "@"
    }

    private fun resetState() {
        pluginInfos.clear()
        intents.clear()
        cacheFlags.clear()
        memberCachePolicies.clear()
        memberCachePolicies += setOf("OWNER", "VOICE")
        guildCommands.clear()
        globalCommands.clear()
        listenersQueue.clear()
        pluginQueue.clear()
        closeClassLoaders()
    }
}

private data class InfoSimple(
    val name: String,
    val mainClass: String,
    val path: Path,
    val depend: Set<String>,
    val softDepend: Set<String>,
    val dependJars: Set<Path>,
    val prefix: String,
    val componentPrefix: String,
)
