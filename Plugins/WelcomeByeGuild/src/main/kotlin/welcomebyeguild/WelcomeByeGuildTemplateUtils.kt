package tw.xinshou.discord.plugin.welcomebyeguild

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import java.awt.Color
import java.time.Instant

internal fun WelcomeByeGuild.handleGuildMemberJoin(event: GuildMemberJoinEvent) {
    val guild = event.guild
    val data = jsonGuildManager.mapper[guild.idLong]?.data ?: return
    ensureDefaultsForMemberEvent(data)

    val channel = guild.getTextChannelById(data.channelId) ?: return

    channel.sendMessageEmbeds(
        createMemberEmbed(
            data = data,
            user = event.user,
            guildName = guild.name,
            memberCount = guild.memberCount,
            isJoin = true,
        )
    ).queue()
}

internal fun WelcomeByeGuild.handleGuildMemberRemove(event: GuildMemberRemoveEvent) {
    val guild = event.guild
    val data = jsonGuildManager.mapper[guild.idLong]?.data ?: return
    ensureDefaultsForMemberEvent(data)

    val channel = guild.getTextChannelById(data.channelId) ?: return

    channel.sendMessageEmbeds(
        createMemberEmbed(
            data = data,
            user = event.user,
            guildName = guild.name,
            memberCount = guild.memberCount,
            isJoin = false,
        )
    ).queue()
}

private fun WelcomeByeGuild.ensureDefaultsForMemberEvent(data: GuildSetting) {
    val defaultJoin = messageCreator.getCreateBuilder(WelcomeByeGuild.MessageKeys.DEFAULT_JOIN, defaultLocale).build().embeds.first()
    val defaultLeave = messageCreator.getCreateBuilder(WelcomeByeGuild.MessageKeys.DEFAULT_LEAVE, defaultLocale).build().embeds.first()

    if (data.welcomeTitle.isBlank()) {
        data.welcomeTitle = requireNotNull(defaultJoin.title)
    }

    if (data.welcomeDescription.isBlank()) {
        data.welcomeDescription = requireNotNull(defaultJoin.description)
    }

    if (data.byeTitle.isBlank()) {
        data.byeTitle = requireNotNull(defaultLeave.title)
    }

    if (data.byeDescription.isBlank()) {
        data.byeDescription = requireNotNull(defaultLeave.description)
    }
}

internal fun WelcomeByeGuild.createMemberEmbed(
    data: GuildSetting,
    user: User,
    guildName: String,
    memberCount: Int,
    isJoin: Boolean,
) = EmbedBuilder().apply {
    val titleTemplate = if (isJoin) data.welcomeTitle else data.byeTitle
    val descriptionTemplate = if (isJoin) data.welcomeDescription else data.byeDescription

    setTitle(parseTemplate(titleTemplate, user, guildName, memberCount))
    setDescription(parseTemplate(descriptionTemplate, user, guildName, memberCount))

    val thumbnail = data.thumbnailUrl.ifBlank { user.effectiveAvatarUrl }
    if (thumbnail.isNotBlank()) {
        setThumbnail(thumbnail)
    }

    if (data.imageUrl.isNotBlank()) {
        setImage(data.imageUrl)
    }

    setColor(Color(if (isJoin) data.welcomeColor else data.byeColor))
    setTimestamp(Instant.now())
}.build()

private fun WelcomeByeGuild.parseTemplate(template: String, user: User, guildName: String, memberCount: Int): String {
    return Placeholder.get(user)
        .putAll(
            "wbg@guild_name" to guildName,
            "wbg@member_count" to memberCount.toString(),
        )
        .parse(template)
}

internal fun WelcomeByeGuild.parseColor(input: String): Int? {
    if (!colorRegex.matches(input)) return null
    return input.removePrefix("#").toInt(16)
}
