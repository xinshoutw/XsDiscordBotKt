package tw.xinshou.plugin.ntustcourse.command

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands

/**
 * Retrieves and constructs an array of guild-specific command configurations for giveaway functionality.
 *
 * @return Array<CommandData> Collection of guild commands for giveaway management.
 */
internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        Commands.slash("course-add", "add a course").addOption(
            OptionType.STRING, "id", "the course code to add", true
        ),
        Commands.slash("course-remove", "remove a course").addOption(
            OptionType.STRING, "id", "the course code to remove", true
        ),
        Commands.slash("course-list", "list all courses"),
    )


internal val commandStringSet: Set<String> = setOf(
    "course-add",
    "course-remove",
    "course-list",
)