package tw.xinshou.discord.plugin.economy

import core.command.CommandHandler
import core.command.ComponentHandler
import core.command.componentHandler
import core.config.ConfigLoader
import core.i18n.Localizer
import core.util.GuildJsonFile
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
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
        pluginDirectory = this.pluginDirectory

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
                JsonImpl.jsonGuildFileManager = GuildJsonFile(
                    directory = File(pluginDirectory, "data"),
                    serializer = MapSerializer(String.serializer(), DataContainer.serializer()),
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
