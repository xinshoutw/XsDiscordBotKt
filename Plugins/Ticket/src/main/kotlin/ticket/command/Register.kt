package tw.xinshou.discord.plugin.ticket.command

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.plugin.ticket.Ticket

private object Keys {
    const val CREATE_TICKET = "createTicket"
    const val CREATE_TICKET_NAME = "$CREATE_TICKET.name"
    const val CREATE_TICKET_DESC = "$CREATE_TICKET.description"
    const val ADD_TICKET = "addTicket"
    const val ADD_TICKET_NAME = "$ADD_TICKET.name"
    const val ADD_TICKET_DESC = "$ADD_TICKET.description"
    private const val AT_OPTIONS = "$ADD_TICKET.options"
    const val AT_OPT_MESSAGE_ID_NAME = "$AT_OPTIONS.messageId.name"
    const val AT_OPT_MESSAGE_ID_DESC = "$AT_OPTIONS.messageId.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("create-ticket", "create custom reason of ticket")
            .setNameLocalizations(localizer[Keys.CREATE_TICKET_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.CREATE_TICKET_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    ) { event -> Ticket.onSlashCommandInteraction(event) },
    slashCommand(
        data = Commands.slash("add-ticket", "create custom reason of ticket in the text-channel")
            .setNameLocalizations(localizer[Keys.ADD_TICKET_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.ADD_TICKET_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addOptions(
                OptionData(OptionType.STRING, "message_id", "for adding extra button", true)
                    .setNameLocalizations(localizer[Keys.AT_OPT_MESSAGE_ID_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.AT_OPT_MESSAGE_ID_DESC].toMap())
            ),
    ) { event -> Ticket.onSlashCommandInteraction(event) },
)
