package ntustcourse.command

import core.command.CommandHandler
import core.command.slashCommand
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import ntustcourse.NtustCourse

/**
 * Retrieves and constructs a list of guild-specific command configurations for course functionality.
 */
internal val guildCommands: List<CommandHandler>
    get() = listOf(
        slashCommand(
            data = Commands.slash("course-add", "add a course").addOption(
                OptionType.STRING, "id", "the course code to add", true
            ),
        ) { event -> NtustCourse.onSlashCommandInteraction(event) },
        slashCommand(
            data = Commands.slash("course-remove", "remove a course").addOption(
                OptionType.STRING, "id", "the course code to remove", true
            ),
        ) { event -> NtustCourse.onSlashCommandInteraction(event) },
        slashCommand(
            data = Commands.slash("course-list", "list all courses"),
        ) { event -> NtustCourse.onSlashCommandInteraction(event) },
    )
