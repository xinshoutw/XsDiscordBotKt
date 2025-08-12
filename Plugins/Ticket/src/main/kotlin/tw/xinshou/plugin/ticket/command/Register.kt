package tw.xinshou.plugin.ticket.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.loader.localizations.StringLocalizer

internal val commandNameSet: Set<String> = setOf(
    "create-ticket",
    "add-ticket",
)

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

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("create-ticket", "create custom reason of ticket")
        .setNameLocalizations(localizer.getLocaleData(Keys.CREATE_TICKET_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.CREATE_TICKET_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    Commands.slash("add-ticket", "create custom reason of ticket in the text-channel")
        .setNameLocalizations(localizer.getLocaleData(Keys.ADD_TICKET_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.ADD_TICKET_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        .addOptions(
            OptionData(OptionType.STRING, "message_id", "for adding extra button", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.AT_OPT_MESSAGE_ID_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.AT_OPT_MESSAGE_ID_DESC))
        ),
)