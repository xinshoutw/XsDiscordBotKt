package tw.xinshou.plugin.botinfo.command

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.plugin.botinfo.command.lang.CmdLocalizations

/**
 * Retrieves and constructs an array of guild-specific command configurations.
 * Each command is tailored for economic transactions like checking balance, modifying balances, and viewing leaderboards.
 *
 * @return Array<CommandData> Collection of guild commands configured with localizations and permissions.
 */
internal val globalCommands: Array<CommandData>
    get() = arrayOf(
        // Command to display bot info message
        Commands.slash("bot-info", "show about the bot data")
            .setNameLocalizations(CmdLocalizations.botInfo.name)
            .setDescriptionLocalizations(CmdLocalizations.botInfo.description)
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
    )
