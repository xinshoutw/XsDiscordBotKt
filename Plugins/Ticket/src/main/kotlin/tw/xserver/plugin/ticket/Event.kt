package tw.xserver.plugin.ticket

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xserver.loader.localizations.LangManager
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.ticket.command.getCommandNameSet
import tw.xserver.plugin.ticket.command.getGuildCommands
import tw.xserver.plugin.ticket.lang.CmdFileSerializer
import tw.xserver.plugin.ticket.lang.CmdLocalizations
import java.io.File

object Event : PluginEvent(true) {
    internal const val COMPONENT_PREFIX = "ticket-v2"
    internal val PLUGIN_DIR_FILE = File("./plugins/Ticket/")

    init {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
    }

    override fun load() {
        reloadLang()
    }

    override fun unload() {}

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("./lang")

        LangManager(
            pluginDirFile = PLUGIN_DIR_FILE,
            fileName = "register.yml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )
    }

    override fun guildCommands(): Array<CommandData> = getGuildCommands()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkEventPrefix(event, getCommandNameSet())) return
        Ticket.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkPrefix(event, COMPONENT_PREFIX)) return
        Ticket.onButtonInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkPrefix(event, COMPONENT_PREFIX)) return
        Ticket.onEntitySelectInteraction(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        Ticket.onGuildLeave(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (GlobalUtil.checkPrefix(event, COMPONENT_PREFIX)) return
        Ticket.onModalInteraction(event)
    }
}
