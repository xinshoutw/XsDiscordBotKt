package tw.xinshou.plugin.simplecommand.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.plugin.simplecommand.command.lang.CmdLocalizations

internal val commandStringSet: Set<String> = setOf(
    "ctcb-none-card",
    "ctcb-remit",
    "chpytwtp-remit"
)

internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        Commands.slash("ctcb-none-card", "generate message")
            .setNameLocalizations(CmdLocalizations.ctcbNoneCard.name)
            .setDescriptionLocalizations(CmdLocalizations.ctcbNoneCard.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
        Commands.slash("ctcb-remit", "generate message")
            .setNameLocalizations(CmdLocalizations.ctcbRemit.name)
            .setDescriptionLocalizations(CmdLocalizations.ctcbRemit.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
        Commands.slash("chpytwtp-remit", "generate message")
            .setNameLocalizations(CmdLocalizations.chpytwtpRemit.name)
            .setDescriptionLocalizations(CmdLocalizations.chpytwtpRemit.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    )

