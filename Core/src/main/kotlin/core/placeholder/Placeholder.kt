package core.placeholder

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.format.DateTimeFormatter

fun Substitutor.withUser(user: User): Substitutor = apply {
    put("user_id", user.id)
    put("user_name", user.name)
    put("user_mention", user.asMention)
    putLazy("user_avatar_url") { user.effectiveAvatarUrl }
    put("user_is_bot", user.isBot.toString())
    put("user_created_unix", user.timeCreated.toEpochSecond().toString())
    put("user_created_iso", user.timeCreated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
}

fun Substitutor.withMember(member: Member?): Substitutor = apply {
    if (member == null) return@apply
    put("member_id", member.id)
    put("member_display_name", member.effectiveName)
    putLazy("member_nickname") { member.nickname ?: member.effectiveName }
    putLazy("member_avatar_url") { member.effectiveAvatarUrl }
    put("member_roles_count", member.roles.size.toString())
    putLazy("member_roles_mentions") { member.roles.joinToString(", ") { it.asMention } }
    putLazy("member_roles_names") { member.roles.joinToString(", ") { it.name } }
    put("member_joined_unix", member.timeJoined.toEpochSecond().toString())
    put("member_joined_iso", member.timeJoined.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
}

fun Substitutor.withGuild(guild: Guild?): Substitutor = apply {
    if (guild == null) return@apply
    put("guild_id", guild.id)
    put("guild_name", guild.name)
    putLazy("guild_member_count") { guild.memberCount.toString() }
    putLazy("guild_icon_url") { guild.iconUrl ?: "" }
    put("guild_owner_id", guild.ownerId)
}

fun Substitutor.withCommand(event: SlashCommandInteractionEvent): Substitutor = apply {
    put("command_name", event.name)
    put("full_command_name", event.fullCommandName)
    put("command_string", event.commandString)
    putLazy("channel_id") { event.channel.id }
    putLazy("channel_name") { event.channel.name }
    event.subcommandName?.let { put("subcommand_name", it) }
}

fun Substitutor.withBot(bot: User): Substitutor = apply {
    put("bot_id", bot.id)
    put("bot_name", bot.name)
}
