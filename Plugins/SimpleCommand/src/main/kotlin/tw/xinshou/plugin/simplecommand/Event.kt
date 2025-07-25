package tw.xinshou.plugin.simplecommand

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.localizations.DiscordLocalizationExporter
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.simplecommand.command.commandStringSet
import tw.xinshou.plugin.simplecommand.command.guildCommands
import tw.xinshou.plugin.simplecommand.command.lang.CmdFileSerializer
import tw.xinshou.plugin.simplecommand.command.lang.CmdLocalizations
import java.io.File

object Event : PluginEvent(true) {
    internal val PLUGIN_DIR_FILE = File("plugins/SimpleCommand")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reload(true)

        logger.info("SimpleCommand loaded.")
    }

    override fun unload() {
        logger.info("SimpleCommand unloaded.")
    }

    override fun reload(init: Boolean) {
        fileGetter.exportDefaultDirectory("lang")

        DiscordLocalizationExporter(
            pluginDirFile = PLUGIN_DIR_FILE,
            fileName = "register.yaml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )

        logger.info("Setting file loaded successfully.")
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        SimpleCommand.onSlashCommandInteraction(event)
    }

    override fun guildCommands(): Array<CommandData> = guildCommands
}
