package tw.xinshou.plugin.giveaway.command

import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands

/**
 * Retrieves and constructs an array of guild-specific command configurations for giveaway functionality.
 *
 * @return Array<CommandData> Collection of guild commands for giveaway management.
 */
internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        // Command to create a new giveaway with configuration interface
        Commands.slash("create-giveaway", "Create a new giveaway with interactive configuration")
        // TODO: Add localization support
        // .setNameLocalizations(CmdLocalizations.createGiveaway.name)
        // .setDescriptionLocalizations(CmdLocalizations.createGiveaway.description)
    )