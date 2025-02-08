package tw.xserver.loader.base

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.plugin.yaml.InfoSerializer
import java.io.File
import java.util.*
import java.util.jar.JarFile

/**
 * Manages the lifecycle of all plugins, loading and unloading them as required.
 */
internal object PluginLoader {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val dir: File = File("./plugins")
    private val pluginInfos: MutableMap<String, InfoSimple> = HashMap()
    val intents: EnumSet<GatewayIntent> = EnumSet.noneOf(GatewayIntent::class.java)
    val cacheFlags: EnumSet<CacheFlag> = EnumSet.noneOf(CacheFlag::class.java)
    val memberCachePolicies: MutableList<String> = mutableListOf()
    val guildCommands = mutableListOf<CommandData>()
    val globalCommands = mutableListOf<CommandData>()
    val listenersQueue = ArrayDeque<PluginEvent>()
    val pluginQueue = LinkedHashMap<String, PluginEvent>()

    /**
     * Loads all plugins from the plugins directory.
     */
    fun preLoad() {
        var success = 0
        var fail = 0
        logger.info("Start loading plugins...")

        val loader = tw.xserver.loader.plugin.ClassLoader
        dir.mkdirs()

        dir.listFiles()?.filter { it.isFile && it.extension == "jar" }?.forEach { file ->
            JarFile(file).use { jarFile ->
                try {
                    jarFile.getInputStream(jarFile.getEntry("info.yaml")).use { inputStream ->
                        val config = Yaml().decodeFromStream<InfoSerializer>(inputStream)
                        logger.debug("-------> {}", config.name)

                        if (pluginInfos.containsKey(config.name)) {
                            logger.error("Duplicate plugin name: {}!", file.name)
                            fail++
                            return
                        }
                        loader.addJar(file, config.main)

                        loader.getClass(config.main)?.let {
                            pluginInfos[config.name] =
                                InfoSimple(config.name, it, config.dependPlugins, config.softDependPlugins)
                            success++
                        } ?: run { fail++ }

                        intents.addAll(config.requireIntents.mapNotNull { runCatching { GatewayIntent.valueOf(it) }.getOrNull() })
                        cacheFlags.addAll(config.requireCacheFlags.mapNotNull { runCatching { CacheFlag.valueOf(it) }.getOrNull() })
                        memberCachePolicies.addAll(config.requireMemberCachePolicies)
                        logger.info("==ADD==> {}", config.name)
                    }
                } catch (e: Exception) {
                    fail++
                    logger.error("Error occurred with file: {}!", file.name, e)
                }
            }
        }

        pluginInfos.values.forEach { info ->
            if (!pluginQueue.containsKey(info.name) && addPluginToQueue(info)) {
                logger.error("Stopped loading plugins due to missing dependencies.")
                return
            }
        }

        pluginQueue.values.forEach { plugin ->
            plugin.guildCommands()?.let { guildCommands.addAll(it) }
            plugin.globalCommands()?.let { globalCommands.addAll(it) }
            if (plugin.listener) listenersQueue.add(plugin)
        }

        if (fail > 0) logger.error("{} plugin(s) failed to load.", fail)
        logger.info("{} plugin(s) loaded successfully.", success)
    }

    /**
     * Run whole plugins
     */
    fun run() {
        pluginQueue.values.reversed().forEach { plugin ->
            plugin.load()
            logger.info("{} load successfully.", plugin.pluginName)
            if (plugin.listener) listenersQueue.add(plugin)
        }
    }

    /**
     * Reloads all plugins by calling their reload methods.
     */
    fun reload() {
        pluginQueue.values.forEach { it.reloadAll() }
    }

    /**
     * Recursively loads a plugin and its dependencies.
     *
     * @param pluginInfo Information about the plugin to load.
     * @return True if there is a failure in dependency loading, false otherwise.
     */
    private fun addPluginToQueue(pluginInfo: InfoSimple?): Boolean {
        // Check if all mandatory dependencies are loaded successfully.
        pluginInfo?.depend?.forEach { depend ->
            // If the dependency is not already loaded, and it exists in the plugin list, try to load it.
            if (pluginQueue.containsKey(depend))
                return@forEach

            if (pluginInfos.containsKey(depend)) {
                // If loading the dependency fails, log and return true to indicate a failure.
                if (addPluginToQueue(pluginInfos[depend])) {
                    logger.error("Failed to load dependency {} for plugin {}.", depend, pluginInfo.name)
                    return true
                }
            } else {
                // If the dependency does not exist, log an error and return true.
                logger.error("Plugin: {} is missing dependency: {}", pluginInfo.name, depend)
                return true
            }
        }

        // Check soft dependencies but do not fail if they are missing.
        pluginInfo?.softDepend?.forEach { depend ->
            if (!pluginQueue.containsKey(depend) && pluginInfos.containsKey(depend)) {
                addPluginToQueue(pluginInfos[depend])
            }
        }

        pluginInfo?.let {
            if (!pluginQueue.containsKey(it.name)) {
                logger.info("Initializing {}.", it.name)
                val event: PluginEvent? = it.pluginInstance.getDeclaredField("INSTANCE").get(null) as PluginEvent
                if (event == null) {
                    logger.error("Cannot get object instance of plugin: {}", it.name)
                    return@let
                }

                pluginQueue[it.name] = (it.pluginInstance.getDeclaredField("INSTANCE").get(null) as PluginEvent).apply {
                    pluginName = it.name
                }
            }
        }

        return false
    }
}

private class InfoSimple(
    val name: String,
    val pluginInstance: Class<*>,
    val depend: List<String>,
    val softDepend: List<String>
)
