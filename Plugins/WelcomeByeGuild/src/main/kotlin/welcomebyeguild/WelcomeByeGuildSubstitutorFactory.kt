package tw.xinshou.discord.plugin.welcomebyeguild

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import java.time.Instant

internal object WelcomeByeGuildSubstitutorFactory {
    private const val NONE = "-"

    fun forCommand(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        setting: GuildSetting,
        selectedChannel: TextChannel? = null,
        oldChannelId: Long = 0L,
    ): Substitutor {
        val substitutor = Placeholder.get(event)

        Placeholder.putGuild(substitutor, guild, prefix = "wbg@")
        Placeholder.putUser(substitutor, event.user, prefix = "wbg@")
        event.member?.let { Placeholder.putMember(substitutor, it, prefix = "wbg@") }
        Placeholder.putEvent(substitutor, eventType = "command", prefix = "wbg@")
        Placeholder.putTextChannel(
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
        val substitutor = member?.let { Placeholder.get(it) } ?: Placeholder.get(user)

        Placeholder.putGuild(substitutor, guild, prefix = "wbg@")
        Placeholder.putUser(substitutor, user, prefix = "wbg@")
        member?.let { Placeholder.putMember(substitutor, it, prefix = "wbg@") }
        Placeholder.putEvent(substitutor, eventType = eventType, prefix = "wbg@")

        applyBoundChannels(substitutor, guild, setting)
        return substitutor
    }

    private fun applyBoundChannels(substitutor: Substitutor, guild: Guild, setting: GuildSetting) {
        val welcomeChannel = guild.getTextChannelById(setting.welcomeChannelId)
        val byeChannel = guild.getTextChannelById(setting.byeChannelId)

        Placeholder.putTextChannel(
            substitutor,
            channel = welcomeChannel,
            prefix = "wbg@",
            baseKey = "welcome_channel",
            noneValue = NONE,
        )
        Placeholder.putTextChannel(
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

        Placeholder.putTextChannel(
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
