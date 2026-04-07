package welcomebyeguild

import core.placeholder.Substitutor
import core.placeholder.withGuild
import core.placeholder.withMember
import core.placeholder.withUser
import core.placeholder.withCommand
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

internal object WelcomeByeGuildSubstitutorFactory {
    private const val NONE = "-"

    fun forCommand(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        setting: GuildSetting,
        selectedChannel: TextChannel? = null,
        oldChannelId: Long = 0L,
    ): Substitutor {
        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withGuild(guild)
            .withCommand(event)

        // WBG-prefixed guild
        putGuild(substitutor, guild, prefix = "wbg@")
        putUser(substitutor, event.user, prefix = "wbg@")
        event.member?.let { putMember(substitutor, it, prefix = "wbg@") }
        putEvent(substitutor, eventType = "command", prefix = "wbg@")
        putTextChannel(
            substitutor,
            channel = selectedChannel,
            prefix = "wbg@",
            baseKey = "channel",
            noneValue = NONE,
        )

        substitutor.putAll(
            "wbg@operator_id" to event.user.id,
            "wbg@operator_name" to event.user.name,
            "wbg@operator_mention" to event.user.asMention,
        )

        applyBoundChannels(substitutor, guild, setting)
        applyOldChannel(substitutor, guild, oldChannelId)
        return substitutor
    }

    fun forMemberEvent(
        guild: Guild,
        setting: GuildSetting,
        user: User,
        member: Member?,
        eventType: String,
    ): Substitutor {
        val substitutor = Substitutor()
            .withUser(user)
            .withMember(member)
            .withGuild(guild)

        putGuild(substitutor, guild, prefix = "wbg@")
        putUser(substitutor, user, prefix = "wbg@")
        member?.let { putMember(substitutor, it, prefix = "wbg@") }
        putEvent(substitutor, eventType = eventType, prefix = "wbg@")

        applyBoundChannels(substitutor, guild, setting)
        return substitutor
    }

    private fun putGuild(substitutor: Substitutor, guild: Guild, prefix: String) {
        substitutor.putAll(
            "${prefix}guild_id" to guild.id,
            "${prefix}guild_name" to guild.name,
            "${prefix}guild_member_count" to guild.memberCount.toString(),
        )
    }

    private fun putUser(substitutor: Substitutor, user: User, prefix: String) {
        substitutor.putAll(
            "${prefix}user_id" to user.id,
            "${prefix}user_name" to user.name,
            "${prefix}user_mention" to user.asMention,
        )
    }

    private fun putMember(substitutor: Substitutor, member: Member, prefix: String) {
        substitutor.putAll(
            "${prefix}member_id" to member.id,
            "${prefix}member_display_name" to member.effectiveName,
        )
    }

    private fun putEvent(substitutor: Substitutor, eventType: String, prefix: String) {
        substitutor.put("${prefix}event_type", eventType)
    }

    private fun putTextChannel(
        substitutor: Substitutor,
        channel: TextChannel?,
        prefix: String,
        baseKey: String,
        noneValue: String,
    ) {
        substitutor.putAll(
            "${prefix}${baseKey}_id" to (channel?.id ?: noneValue),
            "${prefix}${baseKey}_name" to (channel?.name ?: noneValue),
            "${prefix}${baseKey}_mention" to (channel?.asMention ?: noneValue),
        )
    }

    private fun applyBoundChannels(substitutor: Substitutor, guild: Guild, setting: GuildSetting) {
        val welcomeChannel = guild.getTextChannelById(setting.welcomeChannelId)
        val byeChannel = guild.getTextChannelById(setting.byeChannelId)

        putTextChannel(
            substitutor,
            channel = welcomeChannel,
            prefix = "wbg@",
            baseKey = "welcome_channel",
            noneValue = NONE,
        )
        putTextChannel(
            substitutor,
            channel = byeChannel,
            prefix = "wbg@",
            baseKey = "bye_channel",
            noneValue = NONE,
        )

        substitutor.putAll(
            "wbg@welcome_channel_id" to setting.welcomeChannelId.toString(),
            "wbg@welcome_channel_mention" to mentionOrNone(setting.welcomeChannelId),
            "wbg@welcome_channel_is_set" to (setting.welcomeChannelId != 0L).toString(),

            "wbg@bye_channel_id" to setting.byeChannelId.toString(),
            "wbg@bye_channel_mention" to mentionOrNone(setting.byeChannelId),
            "wbg@bye_channel_is_set" to (setting.byeChannelId != 0L).toString(),
        )
    }

    private fun applyOldChannel(substitutor: Substitutor, guild: Guild, oldChannelId: Long) {
        val oldChannel = if (oldChannelId == 0L) null else guild.getTextChannelById(oldChannelId)

        putTextChannel(
            substitutor,
            channel = oldChannel,
            prefix = "wbg@",
            baseKey = "old_channel",
            noneValue = NONE,
        )

        substitutor.putAll(
            "wbg@old_channel_id" to oldChannelId.toString(),
            "wbg@old_channel_mention" to mentionOrNone(oldChannelId),
        )
    }

    private fun mentionOrNone(channelId: Long): String = if (channelId == 0L) NONE else "<#${channelId}>"
}
