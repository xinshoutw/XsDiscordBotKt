package tw.xinshou.plugin.economy

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.json.JsonFileManager
import tw.xinshou.loader.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.loader.localizations.DiscordLocalizationExporter
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.economy.command.commandStringSet
import tw.xinshou.plugin.economy.command.guildCommands
import tw.xinshou.plugin.economy.command.lang.CmdFileSerializer
import tw.xinshou.plugin.economy.command.lang.CmdLocalizations
import tw.xinshou.plugin.economy.json.JsonDataClass
import tw.xinshou.plugin.economy.serializer.MainConfigSerializer
import tw.xinshou.plugin.economy.storage.IStorage
import tw.xinshou.plugin.economy.storage.JsonImpl
import tw.xinshou.plugin.economy.storage.SheetImpl
import java.io.File
import java.io.IOException

/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEvent(true) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val MODE = Mode.Json
    internal const val COMPONENT_PREFIX = "economy@"
    internal val PLUGIN_DIR_FILE = File("plugins/Economy")
    internal lateinit var config: MainConfigSerializer
    internal lateinit var storageManager: IStorage


    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reload(true)

        storageManager.init()
        storageManager.sortMoneyBoard()
        storageManager.sortCostBoard()

        logger.info("Economy loaded.")
    }

    override fun unload() {
        logger.info("Economy unloaded.")
    }

    override fun reload(init: Boolean) {
        try {
            fileGetter.readInputStream("config.yaml").use {
                config = Yaml().decodeFromStream<MainConfigSerializer>(it)
            }
        } catch (e: IOException) {
            logger.error("Please configure {}./config.yaml!", PLUGIN_DIR_FILE.canonicalPath, e)
        }

        logger.info("Setting file loaded successfully.")

        when (MODE) {
            Mode.Json -> {
                val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()

                JsonImpl.jsonFileManager = JsonFileManager(
                    File(PLUGIN_DIR_FILE, "data/data.json"),
                    jsonAdapter,
                    mutableMapOf()
                )
                storageManager = JsonImpl
            }

            Mode.GoogleSheet -> {
                storageManager = SheetImpl
            }
        }


        fileGetter.exportDefaultDirectory("lang")

        DiscordLocalizationExporter(
            pluginDirFile = PLUGIN_DIR_FILE,
            fileName = "register.yaml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )

        if (!init) {
            logger.info("Data file loaded successfully.")
        }
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        Economy.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        Economy.onButtonInteraction(event)
    }

    private enum class Mode {
        Json,
        GoogleSheet,
    }
}
