package tw.xinshou.plugin.ticket

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.localizations.LangManager
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.ticket.command.commandNameSet
import tw.xinshou.plugin.ticket.command.guildCommands
import tw.xinshou.plugin.ticket.lang.CmdFileSerializer
import tw.xinshou.plugin.ticket.lang.CmdLocalizations
import java.io.File

object Event : PluginEvent(true) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal const val COMPONENT_PREFIX = "ticket@"
    internal val PLUGIN_DIR_FILE = File("plugins/Ticket")

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadLang()

        logger.info("Ticket loaded.")
    }

    override fun unload() {
        logger.info("Ticket unloaded.")
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
        if (GlobalUtil.checkSlashCommand(event, commandNameSet)) return
        Ticket.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        Ticket.onButtonInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        Ticket.onEntitySelectInteraction(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (GlobalUtil.checkModalIdPrefix(event, COMPONENT_PREFIX)) return
        Ticket.onModalInteraction(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        Ticket.onGuildLeave(event)
    }
}
