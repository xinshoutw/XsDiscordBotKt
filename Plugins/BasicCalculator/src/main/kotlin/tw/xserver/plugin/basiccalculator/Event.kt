package tw.xserver.plugin.basiccalculator


import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.localizations.LangManager
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.basiccalculator.command.guildCommands
import tw.xserver.plugin.basiccalculator.lang.CmdFileSerializer
import tw.xserver.plugin.basiccalculator.lang.CmdLocalizations
import java.io.File


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEvent(true) {
    internal val PLUGIN_DIR_FILE = File("plugins/BasicCalculator")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadAll()

        logger.info("BasicCalculator loaded.")
    }

    override fun unload() {
        logger.info("BasicCalculator unloaded.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("lang")

        LangManager(
            PLUGIN_DIR_FILE,
            "register.yml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "basic-calculate")) return
        BasicCalculator.onSlashCommandInteraction(event)
    }
}
