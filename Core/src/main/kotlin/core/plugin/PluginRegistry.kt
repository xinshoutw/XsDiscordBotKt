package core.plugin

import com.charleskorn.kaml.Yaml
import core.command.CommandRegistry
import core.command.ComponentRegistry
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

class PluginRegistry(
    private val pluginsDir: File,
    private val commandRegistry: CommandRegistry,
    private val componentRegistry: ComponentRegistry,
) {
    private val logger = LoggerFactory.getLogger(PluginRegistry::class.java)
    private val plugins = linkedMapOf<String, PluginEntry>()
    private val yaml = Yaml.default

    data class PluginEntry(
        val plugin: Plugin,
        val config: PluginConfig,
        val context: PluginContext,
        val koinModule: Module,
        val classLoader: URLClassLoader?,
    )

    // -- Aggregated JDA requirements --

    fun aggregateIntents(): Collection<GatewayIntent> {
        return plugins.values.flatMap { entry ->
            entry.config.requireIntents.map { GatewayIntent.valueOf(it.uppercase()) }
        }.toSet()
    }

    fun aggregateCacheFlags(): Collection<CacheFlag> {
        return plugins.values.flatMap { entry ->
            entry.config.requireCacheFlags.map { CacheFlag.valueOf(it.uppercase()) }
        }.toSet()
    }

    fun aggregateMemberCachePolicy(): MemberCachePolicy {
        val allPolicies = plugins.values.flatMap { it.config.requireMemberCachePolicies }
        return processMemberCachePolicy(allPolicies)
    }

    fun aggregateListeners(): List<Any> =
        plugins.values.flatMap { it.plugin.listeners() }

    // -- Lifecycle --

    suspend fun discoverAndLoad() {
        if (!pluginsDir.isDirectory) {
            logger.warn("Plugins directory does not exist: {}", pluginsDir.absolutePath)
            return
        }

        val jarFiles = pluginsDir.listFiles { f -> f.extension == "jar" } ?: return
        if (jarFiles.isEmpty()) {
            logger.info("No plugin JARs found in {}", pluginsDir.absolutePath)
            return
        }

        // 1. Read configs from each JAR
        val configs = mutableMapOf<String, Pair<PluginConfig, File>>()
        for (jar in jarFiles) {
            try {
                val config = readPluginConfig(jar)
                configs[config.name] = config to jar
            } catch (e: Exception) {
                logger.error("Failed to read plugin config from {}: {}", jar.name, e.message)
            }
        }

        // 2. Validate coreApi compatibility
        val coreVersion = readCoreVersion()
        val compatible = configs.filter { (name, pair) ->
            val (config, _) = pair
            if (isApiCompatible(config.coreApi, coreVersion)) {
                true
            } else {
                logger.error(
                    "Plugin '{}' requires coreApi {} but Core is {}. Skipping.",
                    name, config.coreApi, coreVersion
                )
                false
            }
        }

        // 3. Topological sort by dependPlugins
        val sorted = topologicalSort(compatible)

        // 4. Load each plugin in order
        for (name in sorted) {
            val (config, jarFile) = compatible[name] ?: continue
            try {
                loadPlugin(config, jarFile)
                logger.info("Loaded plugin: {} v{}", config.name, config.version)
            } catch (e: Exception) {
                logger.error("Failed to load plugin '{}': {}", name, e.message, e)
            }
        }
    }

    suspend fun unloadAll() {
        for (name in plugins.keys.reversed()) {
            unloadPlugin(name)
        }
    }

    suspend fun reloadAll() {
        for ((name, entry) in plugins) {
            logger.info("Reloading plugin: {}", name)
            with(entry.plugin) { entry.context.onReload() }
        }
    }

    suspend fun reloadPlugin(name: String) {
        val entry = plugins[name] ?: run {
            logger.warn("Plugin '{}' not found for reload", name)
            return
        }
        logger.info("Reloading plugin: {}", name)
        with(entry.plugin) { entry.context.onReload() }
    }

    // -- Internal helpers --

    private fun readPluginConfig(jarFile: File): PluginConfig {
        JarFile(jarFile).use { jar ->
            val entry = jar.getJarEntry("info.yaml")
                ?: error("No info.yaml found in ${jarFile.name}")
            val content = jar.getInputStream(entry).bufferedReader().readText()
            return yaml.decodeFromString(PluginConfig.serializer(), content)
        }
    }

    private fun readCoreVersion(): String {
        // Read from gradle.properties at build time; fallback to system property or hardcoded
        return System.getProperty("core.version", "4.0")
    }

    private fun isApiCompatible(required: String, current: String): Boolean {
        val reqParts = required.split(".")
        val curParts = current.split(".")
        if (reqParts.size < 2 || curParts.size < 2) return false
        return reqParts[0] == curParts[0] && reqParts[1] == curParts[1]
    }

    private fun topologicalSort(
        configs: Map<String, Pair<PluginConfig, File>>,
    ): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>() // cycle detection

        fun visit(name: String) {
            if (name in visited) return
            if (name in visiting) error("Circular dependency detected involving plugin: $name")

            visiting.add(name)
            val (config, _) = configs[name] ?: run {
                visiting.remove(name)
                return
            }
            for (dep in config.dependPlugins) {
                visit(dep)
            }
            visiting.remove(name)
            visited.add(name)
            result.add(name)
        }

        for (name in configs.keys) {
            visit(name)
        }
        return result
    }

    private fun loadPlugin(config: PluginConfig, jarFile: File) {
        val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), javaClass.classLoader)
        val clazz = classLoader.loadClass(config.main)

        // Try Kotlin object INSTANCE first, then no-arg constructor
        val plugin: Plugin = try {
            val instanceField = clazz.getDeclaredField("INSTANCE")
            instanceField.get(null) as Plugin
        } catch (_: NoSuchFieldException) {
            clazz.getDeclaredConstructor().newInstance() as Plugin
        }

        // Inject config into the plugin instance
        plugin.config = config

        // Create plugin directory
        val pluginDir = File(pluginsDir, config.name).also { it.mkdirs() }

        // Create Koin module from plugin definitions
        val koinModule = module { with(plugin) { definitions() } }
        loadKoinModules(koinModule)

        // Create context and invoke onLoad
        val context = PluginContext(
            pluginName = config.name,
            pluginDirectory = pluginDir,
            logger = LoggerFactory.getLogger("Plugin:${config.name}"),
        )
        with(plugin) { context.onLoad() }

        // Register commands and components
        for (handler in plugin.commands()) {
            commandRegistry.register(handler, config.name)
        }
        for (handler in plugin.components()) {
            componentRegistry.register(handler)
        }

        plugins[config.name] = PluginEntry(plugin, config, context, koinModule, classLoader)
    }

    private fun unloadPlugin(name: String) {
        val entry = plugins.remove(name) ?: return
        logger.info("Unloading plugin: {}", name)

        with(entry.plugin) { entry.context.onUnload() }
        unloadKoinModules(entry.koinModule)
        entry.classLoader?.close()

        // Deregister commands and components
        commandRegistry.deregisterAll(name)
        if (entry.config.componentPrefix.isNotEmpty()) {
            componentRegistry.deregisterByPrefix(entry.config.componentPrefix)
        }
    }
}
