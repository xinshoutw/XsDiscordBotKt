package tw.xinshou.discord.core.dashboard

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.BotLoader
import tw.xinshou.discord.core.base.PluginLoader
import tw.xinshou.discord.core.base.SettingsLoader
import tw.xinshou.discord.core.plugin.yaml.InfoSerializer
import tw.xinshou.discord.core.setting.SettingSerializer
import tw.xinshou.discord.webdashboard.api.DashboardBackend
import tw.xinshou.discord.webdashboard.model.BuiltinSettingsDto
import tw.xinshou.discord.webdashboard.model.ConsoleTargetDto
import tw.xinshou.discord.webdashboard.model.CoreConfigDto
import tw.xinshou.discord.webdashboard.model.GeneralSettingsDto
import tw.xinshou.discord.webdashboard.model.PluginConfigDto
import tw.xinshou.discord.webdashboard.model.PluginYamlDto
import tw.xinshou.discord.webdashboard.model.SaveResponseDto
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object CoreDashboardBackend : DashboardBackend {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val yaml = Yaml()
    private val lock = ReentrantLock()
    private val enabledLineRegex = Regex("""(?m)^enabled\s*:\s*(true|false)\s*(?:#.*)?$""")
    private val pluginNameRegex = Regex("""^[A-Za-z0-9._-]+$""")

    override fun mode(): String = if (isBotOnline()) "live" else "config-only"

    override fun getCoreConfig(): CoreConfigDto = lock.withLock {
        val setting = readCoreSetting()
        setting.toDto()
    }

    override fun saveCoreConfig(payload: CoreConfigDto): SaveResponseDto = lock.withLock {
        val configFile = coreConfigFile()
        val current = readCoreSetting()
        val beforeText = configFile.readText()

        val normalized = current.copy(
            builtinSettings = SettingSerializer.BuiltinSettings(
                statusChangerSetting = SettingSerializer.BuiltinSettings.StatusChangerSetting(
                    activityMessages = payload.builtins.statusMessages
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                ),
                consoleLoggerSetting = payload.builtins.consoleTargets.map {
                    SettingSerializer.BuiltinSettings.ConsoleLoggerSetting(
                        guildId = it.guildId,
                        channelId = it.channelId,
                        logType = it.logTypes,
                        format = it.format,
                    )
                },
            ),
        )

        val nextText = yaml.encodeToString(SettingSerializer.serializer(), normalized)
        writeAtomically(configFile, nextText)

        try {
            BotLoader.reloadCoreSettingsStrict()
        } catch (e: Exception) {
            writeAtomically(configFile, beforeText)
            runCatching { BotLoader.reloadCoreSettingsStrict() }
            throw IllegalStateException(
                "Failed to apply core settings. Changes were rolled back. ${e.message}",
                e
            )
        }

        SaveResponseDto(
            ok = true,
            message = "Core config saved to config.yaml and applied immediately.",
        )
    }

    override fun listPlugins(): List<PluginConfigDto> = lock.withLock {
        val metadata = loadPluginMetadata()
        val names = linkedSetOf<String>()

        names.addAll(metadata.keys.sorted())
        names.addAll(PluginLoader.pluginQueue.keys.sorted())

        pluginsDirectory()
            .listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.filter { File(it, "config.yaml").exists() }
            ?.map { it.name }
            ?.sorted()
            ?.forEach(names::add)

        names.map { pluginName ->
            val pluginConfig = pluginConfigFile(pluginName)
            val content = pluginConfig.takeIf { it.exists() }?.readText()
            val hasEnabled = content?.let(::containsEnabledFlag) ?: false
            val enabled = content?.let(::extractEnabledValue) ?: true
            val meta = metadata[pluginName]

            PluginConfigDto(
                name = pluginName,
                enabled = enabled,
                category = "Plugin",
                description = meta?.description?.takeIf { it.isNotBlank() }
                    ?: "No description found in info.yaml.",
                dependencies = meta?.dependencies ?: emptyList(),
                intents = meta?.intents ?: emptyList(),
                loaded = PluginLoader.pluginQueue.containsKey(pluginName),
                canToggle = hasEnabled,
                configPath = pluginConfig.takeIf { it.exists() }?.absolutePath,
                hasWebEditor = pluginConfig.exists(),
            )
        }.sortedBy { it.name.lowercase() }
    }

    override fun updatePluginEnabled(pluginName: String, enabled: Boolean): SaveResponseDto = lock.withLock {
        val normalizedPluginName = normalizePluginName(pluginName)
        val configFile = pluginConfigFile(normalizedPluginName)
        if (!configFile.exists()) {
            throw NoSuchElementException("Plugin '$normalizedPluginName' does not have config.yaml.")
        }

        val oldText = configFile.readText()
        if (!containsEnabledFlag(oldText)) {
            throw IllegalStateException("Plugin '$normalizedPluginName' config.yaml has no top-level 'enabled' flag.")
        }

        val newText = enabledLineRegex.replaceFirst(oldText, "enabled: $enabled")

        if (newText == oldText) {
            return SaveResponseDto(ok = true, message = "Plugin '$normalizedPluginName' already enabled=$enabled.")
        }

        writeAtomically(configFile, newText)
        val appliedNow = applyPluginReload(normalizedPluginName, configFile, oldText)

        val message = if (appliedNow) {
            "Plugin '$normalizedPluginName' set to enabled=$enabled and reloaded."
        } else {
            "Plugin '$normalizedPluginName' set to enabled=$enabled. It will apply when plugin loads."
        }

        SaveResponseDto(ok = true, message = message)
    }

    override fun getPluginYaml(pluginName: String): PluginYamlDto = lock.withLock {
        val normalizedPluginName = normalizePluginName(pluginName)
        val configFile = pluginConfigFile(normalizedPluginName)
        if (!configFile.exists()) {
            throw NoSuchElementException("Plugin '$normalizedPluginName' does not have config.yaml.")
        }

        val content = configFile.readText()
        PluginYamlDto(
            name = normalizedPluginName,
            yaml = content,
            path = configFile.absolutePath,
            loaded = PluginLoader.pluginQueue.containsKey(normalizedPluginName),
            hasEnabledFlag = containsEnabledFlag(content),
        )
    }

    override fun savePluginYaml(pluginName: String, yaml: String): SaveResponseDto = lock.withLock {
        val normalizedPluginName = normalizePluginName(pluginName)
        val configFile = pluginConfigFile(normalizedPluginName)
        if (!configFile.exists()) {
            throw NoSuchElementException("Plugin '$normalizedPluginName' does not have config.yaml.")
        }

        val newText = yaml.trim()
        if (newText.isBlank()) {
            throw IllegalArgumentException("YAML body cannot be empty.")
        }

        val oldText = configFile.readText()
        if (oldText == newText) {
            return SaveResponseDto(ok = true, message = "No changes for plugin '$normalizedPluginName'.")
        }

        writeAtomically(configFile, "$newText\n")
        val appliedNow = applyPluginReload(normalizedPluginName, configFile, oldText)

        val message = if (appliedNow) {
            "Plugin '$normalizedPluginName' config saved and reloaded."
        } else {
            "Plugin '$normalizedPluginName' config saved. It will apply when plugin loads."
        }

        SaveResponseDto(ok = true, message = message)
    }

    private fun applyPluginReload(pluginName: String, configFile: File, rollbackText: String): Boolean {
        if (!PluginLoader.pluginQueue.containsKey(pluginName)) {
            return false
        }

        return try {
            BotLoader.reloadPluginStrict(pluginName)
            true
        } catch (e: Exception) {
            writeAtomically(configFile, rollbackText)
            runCatching { BotLoader.reloadPluginStrict(pluginName) }
            throw IllegalStateException(
                "Failed to reload plugin '$pluginName'. Changes were rolled back. ${e.message}",
                e
            )
        }
    }

    private fun coreConfigFile(): File = File("config.yaml")

    private fun pluginsDirectory(): File = File("plugins")

    private fun pluginConfigFile(pluginName: String): File = File(pluginsDirectory(), "$pluginName/config.yaml")

    private fun readCoreSetting(): SettingSerializer {
        val configFile = coreConfigFile()
        if (!configFile.exists()) {
            SettingsLoader.run()
        }
        return yaml.decodeFromString(configFile.readText())
    }

    private fun containsEnabledFlag(content: String): Boolean = enabledLineRegex.containsMatchIn(content)

    private fun extractEnabledValue(content: String): Boolean =
        enabledLineRegex.find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.toBooleanStrictOrNull()
            ?: true

    private fun normalizePluginName(pluginName: String): String {
        val normalized = pluginName.trim()
        if (normalized.isBlank() || !pluginNameRegex.matches(normalized)) {
            throw IllegalArgumentException(
                "Invalid plugin name '$pluginName'. Use only letters, numbers, dot, underscore, and dash."
            )
        }
        return normalized
    }

    private fun writeAtomically(target: File, content: String) {
        target.parentFile?.mkdirs()
        val tempFile = File(target.parentFile ?: File("."), "${target.name}.tmp")
        tempFile.writeText(content)

        try {
            Files.move(
                tempFile.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                tempFile.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun loadPluginMetadata(): Map<String, PluginMeta> {
        val result = linkedMapOf<String, PluginMeta>()
        val jars = pluginsDirectory()
            .listFiles()
            ?.filter { it.isFile && it.extension == "jar" }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

        jars.forEach { jar ->
            runCatching {
                JarFile(jar).use { jarFile ->
                    val infoEntry = jarFile.getEntry("info.yaml") ?: return@use
                    jarFile.getInputStream(infoEntry).use { inputStream ->
                        val info = yaml.decodeFromStream<InfoSerializer>(inputStream)
                        result[info.name] = PluginMeta(
                            name = info.name,
                            description = info.description ?: "",
                            dependencies = info.dependPlugins.toList(),
                            intents = info.requireIntents.toList(),
                        )
                    }
                }
            }.onFailure {
                logger.warn("Failed to read info.yaml from plugin jar: {}", jar.name, it)
            }
        }

        return result
    }

    private fun isBotOnline(): Boolean = runCatching { BotLoader.jdaBot }.isSuccess

    private fun SettingSerializer.toDto(): CoreConfigDto {
        val builtins = builtinSettings
        return CoreConfigDto(
            general = GeneralSettingsDto(
                tokenMasked = maskToken(generalSettings.botToken),
            ),
            builtins = BuiltinSettingsDto(
                statusMessages = builtins?.statusChangerSetting?.activityMessages ?: emptyList(),
                consoleTargets = builtins?.consoleLoggerSetting?.map {
                    ConsoleTargetDto(
                        guildId = it.guildId,
                        channelId = it.channelId,
                        logTypes = it.logType,
                        format = it.format,
                    )
                } ?: emptyList(),
            ),
        )
    }

    private fun maskToken(token: String): String {
        if (token.isBlank()) {
            return ""
        }
        return "•".repeat(token.length)
    }
}

private data class PluginMeta(
    val name: String,
    val description: String,
    val dependencies: List<String>,
    val intents: List<String>,
)
