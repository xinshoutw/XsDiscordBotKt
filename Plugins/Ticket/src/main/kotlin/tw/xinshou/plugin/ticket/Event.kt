package tw.xinshou.plugin.ticket

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.loader.localizations.StringLocalizer
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.ticket.command.CmdFileSerializer
import tw.xinshou.plugin.ticket.command.commandNameSet
import tw.xinshou.plugin.ticket.command.guildCommands

object Event : PluginEvent(true) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

    }

    override fun reload() {
        super.reload()

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandNameSet)) return
        Ticket.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Ticket.onButtonInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Ticket.onEntitySelectInteraction(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (GlobalUtil.checkModalIdPrefix(event, componentPrefix)) return
        Ticket.onModalInteraction(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        Ticket.onGuildLeave(event)
    }
}
