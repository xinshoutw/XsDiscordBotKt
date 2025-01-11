package tw.xserver.plugin.feedbacker.command

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xserver.plugin.feedbacker.lang.CmdLocalizations

/**
 * Retrieves and constructs an array of guild-specific command configurations.
 * Each command is tailored for economic transactions like checking balance, modifying balances, and viewing leaderboards.
 *
 * @return Array<CommandData> Collection of guild commands configured with localizations and permissions.
 */
internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        // Command to retrieve the current money balance of a member.
        Commands.slash("feedbacker", "Create a feedback form for a member")
            .setNameLocalizations(CmdLocalizations.feedbacker.name)
            .setDescriptionLocalizations(CmdLocalizations.feedbacker.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member", true)
                    .setNameLocalizations(CmdLocalizations.feedbacker.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.feedbacker.options.member.description)
            ),
    )
