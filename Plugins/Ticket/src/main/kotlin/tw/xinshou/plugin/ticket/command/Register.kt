package tw.xinshou.plugin.ticket.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.plugin.ticket.lang.CmdLocalizations

internal val commandNameSet: Set<String> = setOf(
    "create-ticket",
    "add-ticket",
)

internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        Commands.slash("create-ticket", "create custom reason of ticket")
            .setNameLocalizations(CmdLocalizations.createTicket.name)
            .setDescriptionLocalizations(CmdLocalizations.createTicket.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        Commands.slash("add-ticket", "create custom reason of ticket in the text-channel")
            .setNameLocalizations(CmdLocalizations.addTicket.name)
            .setDescriptionLocalizations(CmdLocalizations.addTicket.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addOptions(
                OptionData(OptionType.STRING, "message_id", "for adding extra button", true)
                    .setNameLocalizations(CmdLocalizations.addTicket.options.messageId.name)
                    .setDescriptionLocalizations(CmdLocalizations.addTicket.options.messageId.description)
            ),
    )
