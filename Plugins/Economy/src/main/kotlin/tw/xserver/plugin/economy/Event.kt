package tw.xserver.plugin.economy

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.json.JsonObjFileManager
import tw.xserver.loader.localizations.LangManager
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.economy.command.commandStringSet
import tw.xserver.plugin.economy.command.guildCommands
import tw.xserver.plugin.economy.lang.CmdFileSerializer
import tw.xserver.plugin.economy.lang.CmdLocalizations
import tw.xserver.plugin.economy.serializer.MainConfigSerializer
import tw.xserver.plugin.economy.storage.IStorage
import tw.xserver.plugin.economy.storage.JsonImpl
import tw.xserver.plugin.economy.storage.SheetImpl
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
        reloadAll()

        storageManager.init()
        storageManager.sortMoneyBoard()
        storageManager.sortCostBoard()

        logger.info("Economy loaded.")
    }

    override fun unload() {
        logger.info("Economy unloaded.")
    }

    override fun reloadConfigFile() {
        try {
            fileGetter.readInputStream("config.yaml").use {
                config = Yaml().decodeFromStream<MainConfigSerializer>(it)
            }
        } catch (e: IOException) {
            logger.error("Please configure {}./config.yaml!", PLUGIN_DIR_FILE.canonicalPath, e)
        }

        logger.info("Setting file loaded successfully.")
        if (File(PLUGIN_DIR_FILE, "data").mkdirs()) {
            logger.info("Default data folder created.")
        }

        when (MODE) {
            Mode.Json -> {
                JsonImpl.json = JsonObjFileManager(File(PLUGIN_DIR_FILE, "data/data.json"))
                storageManager = JsonImpl
            }

            Mode.GoogleSheet -> {
                storageManager = SheetImpl
            }
        }

        logger.info("Data file loaded successfully.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("lang")

        LangManager(
            pluginDirFile = PLUGIN_DIR_FILE,
            fileName = "register.yaml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )
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
