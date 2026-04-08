package tw.xinshou.discord.plugin.economy

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.ComponentHandler
import tw.xinshou.discord.core.command.componentHandler
import tw.xinshou.discord.core.config.ConfigLoader
import tw.xinshou.discord.core.i18n.Localizer
import tw.xinshou.discord.core.util.GuildJsonFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import tw.xinshou.discord.core.plugin.Plugin
import tw.xinshou.discord.core.plugin.PluginConfig
import tw.xinshou.discord.core.plugin.PluginContext
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.economy.command.guildCommands
import tw.xinshou.discord.plugin.economy.config.ConfigSerializer
import tw.xinshou.discord.plugin.economy.json.DataContainer
import tw.xinshou.discord.plugin.economy.json.JsonDataClass
import tw.xinshou.discord.plugin.economy.storage.IStorage
import tw.xinshou.discord.plugin.economy.storage.JsonImpl
import tw.xinshou.discord.plugin.economy.storage.SheetImpl
import java.io.File

object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var localizer: Localizer
    internal lateinit var pluginDirectory: File
    private val MODE = Mode.Json
    internal lateinit var storageManager: IStorage

    override fun PluginContext.onLoad() {
        this@Event.pluginDirectory = pluginDirectory

        pluginConfig = ConfigLoader.load(
            File(pluginDirectory, "config.yaml"),
            "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("Economy is disabled.")
            return
        }

        when (MODE) {
            Mode.Json -> {
                migrateLegacyDataFileIfNeeded()
                @Suppress("UNCHECKED_CAST")
                JsonImpl.jsonGuildFileManager = GuildJsonFile(
                    directory = File(pluginDirectory, "data"),
                    serializer = MapSerializer(String.serializer(), DataContainer.serializer()) as KSerializer<JsonDataClass>,
                    defaultInstance = { mutableMapOf() },
                )
                storageManager = JsonImpl
            }

            Mode.GoogleSheet -> {
                storageManager = SheetImpl
            }
        }

        localizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )

        storageManager.init()
        storageManager.sortMoneyBoard()
        storageManager.sortCostBoard()

        logger.info("Economy loaded.")
    }

    override fun PluginContext.onUnload() {
        logger.info("Economy unloaded.")
    }

    override fun PluginContext.onReload() {
        onLoad()
        if (!pluginConfig.enabled) return
        MessageReplier.reload()
    }

    override fun commands(): List<CommandHandler> {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands(localizer)
    }

    override fun components(): List<ComponentHandler> {
        val prefix = config.componentPrefix
        if (prefix.isBlank()) return emptyList()

        return listOf(
            componentHandler(prefix) {
                onButton = { event -> Economy.onButtonInteraction(event) }
            }
        )
    }

    private fun migrateLegacyDataFileIfNeeded() {
        val dataDirectory = File(pluginDirectory, "data")
        val legacyFile = File(dataDirectory, "data.json")
        if (!legacyFile.exists()) return

        val guildJsonExists = dataDirectory.listFiles()
            ?.any { it.isFile && it.extension == "json" && it.nameWithoutExtension.toLongOrNull() != null }
            ?: false

        if (guildJsonExists) {
            backupLegacyFile(legacyFile)
            return
        }

        backupLegacyFile(legacyFile)
    }

    private fun backupLegacyFile(legacyFile: File) {
        val backupFile = File(legacyFile.parentFile, "data.legacy.bak")
        if (backupFile.exists()) backupFile.delete()
        legacyFile.renameTo(backupFile)
    }
}
