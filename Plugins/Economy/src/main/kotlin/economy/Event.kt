package tw.xinshou.discord.plugin.economy

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.base.BotLoader.jdaBot
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.economy.command.CmdFileSerializer
import tw.xinshou.discord.plugin.economy.command.commandStringSet
import tw.xinshou.discord.plugin.economy.command.guildCommands
import tw.xinshou.discord.plugin.economy.config.ConfigSerializer
import tw.xinshou.discord.plugin.economy.json.JsonDataClass
import tw.xinshou.discord.plugin.economy.storage.IStorage
import tw.xinshou.discord.plugin.economy.storage.JsonImpl
import tw.xinshou.discord.plugin.economy.storage.SheetImpl
import java.io.File


object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>
    private val MODE = Mode.Json
    internal lateinit var storageManager: IStorage

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("Economy is disabled.")
            return
        }

        when (MODE) {
            Mode.Json -> {
                migrateLegacyDataFileIfNeeded()
                val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()

                JsonImpl.jsonGuildFileManager = JsonGuildFileManager(
                    dataDirectory = File(pluginDirectory, "data"),
                    adapter = jsonAdapter,
                    defaultInstance = mutableMapOf()
                )
                storageManager = JsonImpl
            }

            Mode.GoogleSheet -> {
                storageManager = SheetImpl
            }
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        storageManager.init()
        storageManager.sortMoneyBoard()
        storageManager.sortCostBoard()

        logger.info("Economy loaded.")
    }

    override fun unload() {
        logger.info("Economy unloaded.")
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            logger.warn("Economy is disabled.")
            return
        }

        when (MODE) {
            Mode.Json -> {
                migrateLegacyDataFileIfNeeded()
                val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()

                JsonImpl.jsonGuildFileManager = JsonGuildFileManager(
                    dataDirectory = File(pluginDirectory, "data"),
                    adapter = jsonAdapter,
                    defaultInstance = mutableMapOf()
                )
                storageManager = JsonImpl
            }

            Mode.GoogleSheet -> {
                storageManager = SheetImpl
            }
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        storageManager.init()
        storageManager.sortMoneyBoard()
        storageManager.sortCostBoard()

        MessageReplier.reload()
    }

    override fun guildCommands(): Array<CommandData> {
        return if (!config.enabled) {
            emptyArray()
        } else {
            guildCommands(localizer)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        Economy.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Economy.onButtonInteraction(event)
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
            logger.warn("Legacy economy data file moved to backup because guild json files already exist.")
            return
        }

        val guilds = jdaBot.guilds
        if (guilds.size == 1) {
            val migratedFile = File(dataDirectory, "${guilds.first().idLong}.json")
            if (legacyFile.renameTo(migratedFile)) {
                logger.info("Migrated legacy economy data.json to {}.", migratedFile.name)
            } else {
                logger.warn("Failed to migrate legacy economy data.json. Keeping backup file.")
                backupLegacyFile(legacyFile)
            }
            return
        }

        backupLegacyFile(legacyFile)
        logger.warn(
            "Legacy economy data.json could not be auto-mapped (guild count: {}). File moved to backup.",
            guilds.size
        )
    }

    private fun backupLegacyFile(legacyFile: File) {
        val backupFile = File(legacyFile.parentFile, "data.legacy.bak")
        if (backupFile.exists()) {
            backupFile.delete()
        }
        legacyFile.renameTo(backupFile)
    }
}
