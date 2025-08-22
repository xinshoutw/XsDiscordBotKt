package tw.xinshou.plugin.economy

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.json.JsonFileManager
import tw.xinshou.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.core.localizations.StringLocalizer
import tw.xinshou.core.plugin.PluginEventConfigure
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.economy.command.CmdFileSerializer
import tw.xinshou.plugin.economy.command.commandStringSet
import tw.xinshou.plugin.economy.command.guildCommands
import tw.xinshou.plugin.economy.config.ConfigSerializer
import tw.xinshou.plugin.economy.json.JsonDataClass
import tw.xinshou.plugin.economy.storage.IStorage
import tw.xinshou.plugin.economy.storage.JsonImpl
import tw.xinshou.plugin.economy.storage.SheetImpl
import java.io.File


object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>
    private val MODE = Mode.Json
    internal lateinit var storageManager: IStorage

    override fun load() {
        super.load()

        when (MODE) {
            Mode.Json -> {
                val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()

                JsonImpl.jsonFileManager = JsonFileManager(
                    File(pluginDirectory, "data/data.json"),
                    jsonAdapter,
                    mutableMapOf()
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

        when (MODE) {
            Mode.Json -> {
                val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()

                JsonImpl.jsonFileManager = JsonFileManager(
                    File(pluginDirectory, "data/data.json"),
                    jsonAdapter,
                    mutableMapOf()
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
    }

    override fun guildCommands(): Array<CommandData> = guildCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        Economy.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Economy.onButtonInteraction(event)
    }
}
