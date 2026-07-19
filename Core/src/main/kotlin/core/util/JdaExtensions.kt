package tw.xinshou.discord.core.util

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

// ── User extensions ──

/**
 * Returns a display name for this user in the given guild.
 * If the user has a guild nickname, formats as "nickname (username)";
 * otherwise returns just the username.
 */
fun User.displayName(guild: Guild): String {
    val member = guild.getMember(this)
    return if (member?.nickname != null) "${member.nickname} ($name)" else name
}

// ── Slash command matching (replaces GlobalUtil.checkCommandString, now returns TRUE on match) ──

/** Returns `true` if this event's full command name matches [fullName] exactly. */
fun SlashCommandInteractionEvent.isCommand(fullName: String): Boolean =
    fullCommandName == fullName

/** Returns `true` if this event's full command name is contained in [names]. */
fun SlashCommandInteractionEvent.isCommandIn(names: Set<String>): Boolean =
    fullCommandName in names

// ── Component ID prefix matching ──

/** Returns `true` if this button's component ID starts with [prefix]. */
fun ButtonInteractionEvent.hasPrefix(prefix: String): Boolean =
    componentId.startsWith(prefix)

/** Returns `true` if this modal's ID starts with [prefix]. */
fun ModalInteractionEvent.hasPrefix(prefix: String): Boolean =
    modalId.startsWith(prefix)

/** Returns `true` if this string select menu's component ID starts with [prefix]. */
fun StringSelectInteractionEvent.hasPrefix(prefix: String): Boolean =
    componentId.startsWith(prefix)

/** Returns `true` if this entity select menu's component ID starts with [prefix]. */
fun EntitySelectInteractionEvent.hasPrefix(prefix: String): Boolean =
    componentId.startsWith(prefix)

// ── Convenient reply helpers ──

/** Replies ephemerally with the given [content], queuing the response. */
fun SlashCommandInteractionEvent.replyEphemeral(content: String) {
    reply(content).setEphemeral(true).queue()
}

/** Replies ephemerally with the given [content], queuing the response. */
fun ButtonInteractionEvent.replyEphemeral(content: String) {
    reply(content).setEphemeral(true).queue()
}
