package tw.xinshou.discord.core.plugin

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.BotLoader
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.exists

internal object ClassLoader {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val pluginLoaders: MutableMap<String, PluginSandboxClassLoader> = linkedMapOf()
    private val coreClassLoader: java.lang.ClassLoader = BotLoader::class.java.classLoader

    fun createPluginLoader(
        pluginName: String,
        primaryJar: Path,
        dependencyPluginNames: Set<String> = emptySet(),
        additionalJars: Set<Path> = emptySet(),
    ) {
        if (pluginName in pluginLoaders) {
            throw IllegalStateException("Plugin class loader already exists: $pluginName")
        }

        require(primaryJar.exists()) { "Primary plugin jar does not exist: $primaryJar" }
        additionalJars.forEach { require(it.exists()) { "Dependency jar does not exist: $it" } }

        val urls = linkedSetOf(primaryJar.toUri().toURL())
            .apply { addAll(additionalJars.map { it.toUri().toURL() }) }
            .toTypedArray()

        val dependencyNames = dependencyPluginNames.toSet()
        dependencyNames.forEach { depend ->
            require(depend in pluginLoaders) {
                "Plugin '$pluginName' depends on '$depend', but the dependency class loader is unavailable."
            }
        }

        pluginLoaders[pluginName] = PluginSandboxClassLoader(
            pluginName = pluginName,
            urls = urls,
            parent = coreClassLoader,
            dependencyLoaders = { dependencyNames.mapNotNull(pluginLoaders::get) }
        )
    }

    fun getClass(pluginName: String, className: String): Class<*>? {
        val loader = pluginLoaders[pluginName]
            ?: throw IllegalStateException("Plugin class loader not found: $pluginName")

        return try {
            loader.loadClass(className)
        } catch (e: ClassNotFoundException) {
            logger.error("Class not found in plugin {}: {}", pluginName, className)
            null
        }
    }

    fun clear() {
        pluginLoaders.values.forEach { loader ->
            runCatching { loader.close() }
                .onFailure { logger.warn("Failed to close class loader for {}", loader.pluginName, it) }
        }
        pluginLoaders.clear()
    }
}

private class PluginSandboxClassLoader(
    val pluginName: String,
    urls: Array<URL>,
    parent: java.lang.ClassLoader,
    private val dependencyLoaders: () -> List<PluginSandboxClassLoader>,
) : URLClassLoader(urls, parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let { return resolveIfNeeded(it, resolve) }

            if (shouldParentFirst(name)) {
                runCatching { parent.loadClass(name) }.getOrNull()
                    ?.let { return resolveIfNeeded(it, resolve) }
            }

            runCatching { findClass(name) }.getOrNull()
                ?.let { return resolveIfNeeded(it, resolve) }

            loadFromDependencies(name, resolve)?.let { return it }

            if (!shouldParentFirst(name)) {
                runCatching { parent.loadClass(name) }.getOrNull()
                    ?.let { return resolveIfNeeded(it, resolve) }
            }

            throw ClassNotFoundException(name)
        }
    }

    override fun getResource(name: String): URL? {
        findResource(name)?.let { return it }
        findResourceInDependencies(name, mutableSetOf(this))?.let { return it }
        return parent.getResource(name)
    }

    private fun loadFromDependencies(name: String, resolve: Boolean): Class<*>? {
        val visited = mutableSetOf(this)
        dependencyLoaders().forEach { dependency ->
            dependency.tryLoadClassInSandbox(name, resolve, visited)?.let { return it }
        }
        return null
    }

    internal fun tryLoadClassInSandbox(
        name: String,
        resolve: Boolean,
        visited: MutableSet<PluginSandboxClassLoader>,
    ): Class<*>? {
        if (!visited.add(this)) return null

        synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let { return resolveIfNeeded(it, resolve) }

            if (shouldParentFirst(name)) {
                runCatching { parent.loadClass(name) }.getOrNull()
                    ?.let { return resolveIfNeeded(it, resolve) }
            }

            runCatching { findClass(name) }.getOrNull()
                ?.let { return resolveIfNeeded(it, resolve) }

            dependencyLoaders().forEach { dependency ->
                dependency.tryLoadClassInSandbox(name, resolve, visited)?.let { return it }
            }

            if (!shouldParentFirst(name)) {
                runCatching { parent.loadClass(name) }.getOrNull()
                    ?.let { return resolveIfNeeded(it, resolve) }
            }
        }

        return null
    }

    private fun findResourceInDependencies(
        name: String,
        visited: MutableSet<PluginSandboxClassLoader>,
    ): URL? {
        dependencyLoaders().forEach { dependency ->
            dependency.tryFindResourceInSandbox(name, visited)?.let { return it }
        }
        return null
    }

    internal fun tryFindResourceInSandbox(
        name: String,
        visited: MutableSet<PluginSandboxClassLoader>,
    ): URL? {
        if (!visited.add(this)) return null

        findResource(name)?.let { return it }
        dependencyLoaders().forEach { dependency ->
            dependency.tryFindResourceInSandbox(name, visited)?.let { return it }
        }
        return null
    }

    private fun resolveIfNeeded(clazz: Class<*>, resolve: Boolean): Class<*> {
        if (resolve) resolveClass(clazz)
        return clazz
    }

    private fun shouldParentFirst(name: String): Boolean {
        return PARENT_FIRST_PREFIXES.any(name::startsWith)
    }

    companion object {
        private val PARENT_FIRST_PREFIXES = listOf(
            "java.",
            "javax.",
            "jdk.",
            "kotlin.",
            "kotlinx.",
            "sun.",
            "com.sun.",
            "org.slf4j.",
            "org.jetbrains.",
            "net.dv8tion.jda.",
            "tw.xinshou.discord.core.",
        )
    }
}
