package tw.xinshou.core.util

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.requests.restaction.CacheRestAction
import tw.xinshou.core.base.BotLoader

/**
 * A utility class for fetching and managing entities like Users and Members from JDA.
 */
object GlobalUtil {
    /**
     * Retrieves a User object from Discord's servers by their ID.
     *
     * @param id The unique ID of the user.
     * @return The User object.
     */
    fun getUserById(id: Long): CacheRestAction<User> = BotLoader.jdaBot.retrieveUserById(id)
    fun getUserById(id: String): CacheRestAction<User> = BotLoader.jdaBot.retrieveUserById(id)

    /**
     * Gets the nickname of a user in a guild or their username if the nickname is not available.
     *
     * @param user The user whose name is being retrieved.
     * @param guild The guild from which to retrieve the member's nickname.
     * @return The nickname or username of the user.
     */
    fun getNickOrName(user: User, guild: Guild): String {
        val member: Member? = guild.retrieveMemberById(user.idLong).complete()
        return member?.nickname?.let { "$it (${user.name})" } ?: user.name
    }

    /**
     * Checks if the command name of the given SlashCommandInteractionEvent matches the provided name.
     *
     * @param event The SlashCommandInteractionEvent to check.
     * @param fullName The name to compare with the event's command name.
     * @return True if the names do not match, false otherwise.
     */
    fun checkCommandString(event: SlashCommandInteractionEvent, fullName: String): Boolean =
        event.fullCommandName != fullName

    fun checkSlashCommand(event: SlashCommandInteractionEvent, allowedList: Set<String>): Boolean =
        event.name !in allowedList

    fun checkComponentIdPrefix(event: EntitySelectInteractionEvent, prefix: String): Boolean =
        !event.componentId.startsWith(prefix)

    fun checkComponentIdPrefix(event: StringSelectInteractionEvent, prefix: String): Boolean =
        !event.componentId.startsWith(prefix)

    fun checkComponentIdPrefix(event: ButtonInteractionEvent, prefix: String): Boolean =
        !event.componentId.startsWith(prefix)

    fun checkModalIdPrefix(event: ModalInteractionEvent, prefix: String): Boolean =
        !event.modalId.startsWith(prefix)
}
