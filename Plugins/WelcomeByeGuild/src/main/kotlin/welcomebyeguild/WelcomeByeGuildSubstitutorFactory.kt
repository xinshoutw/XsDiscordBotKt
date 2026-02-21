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

    private const val WELCOME_COLOR = "#57F287"
    private const val BYE_COLOR = "#ED4245"

    private const val WELCOME_PHOTO_URL =
        "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=1600&q=80"

    private const val BYE_PHOTO_URL =
        "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?auto=format&fit=crop&w=1600&q=80"

    fun forCommand(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        setting: GuildSetting,
        selectedChannel: TextChannel? = null,
        oldChannelId: Long = 0L,
    ): Substitutor {
        val substitutor = Placeholder.get(event)

        Placeholder.putGuild(substitutor, guild, prefix = "wbg@", noneValue = NONE)
        Placeholder.putUser(substitutor, event.user, event.member, prefix = "wbg@", noneValue = NONE)
        Placeholder.putMember(substitutor, event.user, event.member, prefix = "wbg@", noneValue = NONE)
        Placeholder.putEvent(substitutor, eventType = "command", prefix = "wbg@", instant = Instant.now())
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
        applyVisuals(substitutor)
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

        Placeholder.putGuild(substitutor, guild, prefix = "wbg@", noneValue = NONE)
        Placeholder.putUser(substitutor, user, member, prefix = "wbg@", noneValue = NONE)
        Placeholder.putMember(substitutor, user, member, prefix = "wbg@", noneValue = NONE)
        Placeholder.putEvent(substitutor, eventType = eventType, prefix = "wbg@", instant = Instant.now())

        applyBoundChannels(substitutor, guild, setting)
        applyVisuals(substitutor)
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

    private fun applyVisuals(substitutor: Substitutor) {
        substitutor.putAll(
            "wbg@welcome_color" to WELCOME_COLOR,
            "wbg@bye_color" to BYE_COLOR,
            "wbg@welcome_photo_url" to WELCOME_PHOTO_URL,
            "wbg@bye_photo_url" to BYE_PHOTO_URL,
        )
    }

    private fun mentionOrNone(channelId: Long): String = if (channelId == 0L) NONE else "<#${channelId}>"
}
