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
        val now = Instant.now()

        substitutor.putAll(buildGuildMap(guild, setting))
        substitutor.putAll(buildUserMap(event.user, event.member))
        substitutor.putAll(buildMemberMap(event.user, event.member))
        substitutor.putAll(buildEventMap(now, "command"))
        substitutor.putAll(
            mapOf(
                "wbg@operator_id" to event.user.id,
                "wbg@operator_name" to event.user.name,
                "wbg@operator_mention" to event.user.asMention,

                "wbg@channel_id" to (selectedChannel?.id ?: "0"),
                "wbg@channel_name" to (selectedChannel?.name ?: NONE),
                "wbg@channel_mention" to (selectedChannel?.asMention ?: NONE),
                "wbg@channel_type" to (selectedChannel?.type?.name ?: NONE),
            )
        )

        substitutor.putAll(buildOldChannelMap(guild, oldChannelId))
        substitutor.putAll(buildVisualMap())
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
        val now = Instant.now()

        substitutor.putAll(buildGuildMap(guild, setting))
        substitutor.putAll(buildUserMap(user, member))
        substitutor.putAll(buildMemberMap(user, member))
        substitutor.putAll(buildEventMap(now, eventType))
        substitutor.putAll(buildVisualMap())

        return substitutor
    }

    private fun buildGuildMap(guild: Guild, setting: GuildSetting): Map<String, String> {
        val owner = guild.owner
        val systemChannel = guild.systemChannel
        val rulesChannel = guild.rulesChannel

        return mapOf(
            "wbg@guild_id" to guild.id,
            "wbg@guild_name" to guild.name,
            "wbg@guild_name_upper" to guild.name.uppercase(),
            "wbg@guild_name_lower" to guild.name.lowercase(),
            "wbg@guild_locale" to guild.locale.name,
            "wbg@guild_member_count" to guild.memberCount.toString(),
            "wbg@guild_booster_count" to guild.boostCount.toString(),
            "wbg@guild_icon_url" to normalize(guild.iconUrl),
            "wbg@guild_banner_url" to normalize(guild.bannerUrl),
            "wbg@guild_description" to normalize(guild.description),
            "wbg@guild_owner_id" to (owner?.id ?: "0"),
            "wbg@guild_owner_name" to (owner?.effectiveName ?: NONE),
            "wbg@guild_owner_mention" to (owner?.asMention ?: NONE),
            "wbg@guild_system_channel_id" to (systemChannel?.id ?: "0"),
            "wbg@guild_system_channel_name" to (systemChannel?.name ?: NONE),
            "wbg@guild_system_channel_mention" to (systemChannel?.asMention ?: NONE),
            "wbg@guild_rules_channel_id" to (rulesChannel?.id ?: "0"),
            "wbg@guild_rules_channel_name" to (rulesChannel?.name ?: NONE),
            "wbg@guild_rules_channel_mention" to (rulesChannel?.asMention ?: NONE),
        ) + buildBoundChannelMap(guild, setting)
    }

    private fun buildUserMap(user: User, member: Member?): Map<String, String> {
        val effectiveName = member?.effectiveName ?: user.name

        return mapOf(
            "wbg@user_id" to user.id,
            "wbg@user_name" to user.name,
            "wbg@user_name_upper" to user.name.uppercase(),
            "wbg@user_name_lower" to user.name.lowercase(),
            "wbg@user_global_name" to normalize(user.globalName),
            "wbg@user_effective_name" to effectiveName,
            "wbg@user_mention" to user.asMention,
            "wbg@user_avatar_url" to normalize(user.effectiveAvatarUrl),
            "wbg@user_default_avatar_url" to normalize(user.defaultAvatarUrl),
            "wbg@user_is_bot" to user.isBot.toString(),
            "wbg@user_created_unix" to user.timeCreated.toEpochSecond().toString(),
            "wbg@user_created_iso" to user.timeCreated.toString(),
        )
    }

    private fun buildMemberMap(user: User, member: Member?): Map<String, String> {
        val roles = member?.roles ?: emptyList()
        val roleMentions = roles.joinToString(separator = " ") { it.asMention }.ifBlank { NONE }
        val roleNames = roles.joinToString(separator = ", ") { it.name }.ifBlank { NONE }

        return mapOf(
            "wbg@member_id" to (member?.id ?: user.id),
            "wbg@member_display_name" to (member?.effectiveName ?: user.name),
            "wbg@member_nickname" to normalize(member?.nickname),
            "wbg@member_nickname_or_name" to (member?.nickname ?: member?.effectiveName ?: user.name),
            "wbg@member_avatar_url" to normalize(member?.effectiveAvatarUrl ?: user.effectiveAvatarUrl),
            "wbg@member_roles_count" to roles.size.toString(),
            "wbg@member_roles_mentions" to roleMentions,
            "wbg@member_roles_names" to roleNames,
            "wbg@member_joined_unix" to (member?.timeJoined?.toEpochSecond()?.toString() ?: "0"),
            "wbg@member_joined_iso" to (member?.timeJoined?.toString() ?: NONE),
        )
    }

    private fun buildEventMap(now: Instant, eventType: String): Map<String, String> = mapOf(
        "wbg@event_type" to eventType,
        "wbg@event_timestamp_unix" to now.epochSecond.toString(),
        "wbg@event_timestamp_ms" to now.toEpochMilli().toString(),
        "wbg@event_timestamp_iso" to now.toString(),
    )

    private fun buildBoundChannelMap(guild: Guild, setting: GuildSetting): Map<String, String> {
        val welcomeChannel = guild.getTextChannelById(setting.welcomeChannelId)
        val byeChannel = guild.getTextChannelById(setting.byeChannelId)

        return mapOf(
            "wbg@welcome_channel_id" to setting.welcomeChannelId.toString(),
            "wbg@welcome_channel_name" to (welcomeChannel?.name ?: NONE),
            "wbg@welcome_channel_mention" to mentionOrNone(setting.welcomeChannelId),
            "wbg@welcome_channel_is_set" to (setting.welcomeChannelId != 0L).toString(),

            "wbg@bye_channel_id" to setting.byeChannelId.toString(),
            "wbg@bye_channel_name" to (byeChannel?.name ?: NONE),
            "wbg@bye_channel_mention" to mentionOrNone(setting.byeChannelId),
            "wbg@bye_channel_is_set" to (setting.byeChannelId != 0L).toString(),
        )
    }

    private fun buildOldChannelMap(guild: Guild, oldChannelId: Long): Map<String, String> {
        val oldChannel = if (oldChannelId == 0L) null else guild.getTextChannelById(oldChannelId)

        return mapOf(
            "wbg@old_channel_id" to oldChannelId.toString(),
            "wbg@old_channel_name" to (oldChannel?.name ?: NONE),
            "wbg@old_channel_mention" to mentionOrNone(oldChannelId),
        )
    }

    private fun buildVisualMap(): Map<String, String> = mapOf(
        "wbg@welcome_color" to WELCOME_COLOR,
        "wbg@bye_color" to BYE_COLOR,
        "wbg@welcome_photo_url" to WELCOME_PHOTO_URL,
        "wbg@bye_photo_url" to BYE_PHOTO_URL,
    )

    private fun normalize(value: String?): String = value?.ifBlank { NONE } ?: NONE

    private fun mentionOrNone(channelId: Long): String = if (channelId == 0L) NONE else "<#${channelId}>"
}
