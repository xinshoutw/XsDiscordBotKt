package tw.xinshou.plugin.rentsystem

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.localizations.DiscordLocalizationExporter
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.rentsystem.command.commandStringSet
import tw.xinshou.plugin.rentsystem.command.guildCommands
import tw.xinshou.plugin.rentsystem.command.lang.CmdFileSerializer
import tw.xinshou.plugin.rentsystem.command.lang.CmdLocalizations
import tw.xinshou.plugin.rentsystem.serializer.MainConfigSerializer
import java.io.File
import java.io.IOException

object Event : PluginEvent(true) {
    internal const val COMPONENT_PREFIX = "rent-system@"
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal val PLUGIN_DIR_FILE = File("plugins/RentSystem")
    internal lateinit var config: MainConfigSerializer

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reload(true)
        RentSystem.start()
        logger.info("RentSystem loaded.")
    }

    override fun unload() {
        RentSystem.stop()
        logger.info("RentSystem unloaded.")
    }

    override fun reload(init: Boolean) {
        try {
            fileGetter.readInputStream("config.yaml").use {
                config = Yaml().decodeFromStream<MainConfigSerializer>(it)
            }
        } catch (e: IOException) {
            logger.error("Please configure {}./config.yaml!", PLUGIN_DIR_FILE.canonicalPath, e)
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
            logger.info("Setting file loaded successfully.")
        }
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        RentSystem.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        RentSystem.onButtonInteraction(event)
    }
}
