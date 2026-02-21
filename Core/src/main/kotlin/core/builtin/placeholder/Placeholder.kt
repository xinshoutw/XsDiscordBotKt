package tw.xinshou.discord.core.builtin.placeholder

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectInteraction
import tw.xinshou.discord.core.base.BotLoader.jdaBot
import java.time.Instant

object Placeholder {
    val globalSubstitutor: Substitutor = Substitutor(
        "bot_id" to jdaBot.selfUser.id,
        "bot_name" to jdaBot.selfUser.name,
    )
    private val userPlaceholder: MutableMap<Long, Substitutor> = HashMap()
    private val memberPlaceholder: MutableMap<Long, Substitutor> = HashMap()

    fun update(user: User, vararg placeholders: Map<String, String>) {
        get(user).putAll(placeholders.flatMap { it.entries }.associate { it.key to it.value })
    }

    fun update(user: User, vararg placeholders: Pair<String, String>) {
        get(user).putAll(placeholders.toMap())
    }

    fun get(user: User): Substitutor {
        return userPlaceholder.getOrPut(
            user.idLong
        ) {
            Substitutor(globalSubstitutor, "user_id" to user.id)
        }.apply { // force update some changeable data
            putAll(
                "user_mention" to user.asMention,
                "user_name" to user.name,
                "user_avatar_url" to user.effectiveAvatarUrl,
            )
        }
    }

    fun get(member: Member): Substitutor {
        return memberPlaceholder.getOrPut(
            member.idLong,
        ) { Substitutor(globalSubstitutor, "user_id" to member.id) }.apply { // force update some changeable data
            putAll(
                "user_mention" to member.asMention,
                "user_name" to member.user.name,
                "user_avatar_url" to member.user.effectiveAvatarUrl,
                "member_display_name" to member.effectiveName,
                "member_avatar_url" to member.effectiveAvatarUrl,
                "member_fixed_name" to  // "nickname (username)" or "username"
                        (member.nickname?.let { "${member.nickname} (${member.user.name})" } ?: member.user.name)
            )
        }
    }

    fun get(event: SlashCommandInteractionEvent): Substitutor {
        return (event.member?.let { get(it) } ?: get(event.user)).apply {
            putAll(
                "command_name" to event.name,
                "full_command_name" to event.fullCommandName,
                "language" to event.userLocale.nativeName,
                "command_string" to event.commandString,
            )
            if (event.isFromGuild) {
                event.guildChannel.let {
                    putAll(
                        "channel_id" to it.id,
                        "channel_name" to it.name,
                    )

                    it.asTextChannel().parentCategory?.let { category ->
                        put("channel_category_name" to category.name)
                    }
                }
            }

            event.subcommandName?.let { put("subcommand_name", it) }
        }
    }

    fun get(event: EntitySelectInteraction): Substitutor {
        return (event.member?.let { get(it) } ?: get(event.user)).apply {
            putAll(
                "component_id" to event.componentId,
                "component_min" to event.component.minValues.toString(),
                "component_max" to event.component.maxValues.toString(),
                "language" to event.userLocale.nativeName,
            )
            event.channel.let {
                putAll(
                    "channel_id" to it.id,
                    "channel_name" to it.name,
                )
            }
            event.component.placeholder?.let { put("component_placeholder", it) }
        }
    }

    fun get(event: StringSelectInteractionEvent): Substitutor {
        return (event.member?.let { get(it) } ?: get(event.user)).apply {
            putAll(
                "component_id" to event.componentId,
                "component_min" to event.component.minValues.toString(),
                "component_max" to event.component.maxValues.toString(),
                "language" to event.userLocale.nativeName,
            )
            event.channel.let {
                putAll(
                    "channel_id" to it.id,
                    "channel_name" to it.name,
                )
            }
            event.component.placeholder?.let { put("component_placeholder", it) }
        }
    }

    fun get(event: ButtonInteractionEvent): Substitutor {
        return (event.member?.let { get(it) } ?: get(event.user)).apply {
            putAll(
                "component_id" to event.componentId,
                "component_label" to event.component.label,
                "component_style" to event.component.style.name,
                "language" to event.userLocale.nativeName,
            )
            event.channel.let {
                putAll(
                    "channel_id" to it.id,
                    "channel_name" to it.name,
                )
            }
            event.component.url?.let { put("component_url", it) }
            event.component.emoji?.let {
                putAll(
                    "component_emoji" to it.formatted,
                    "component_emoji_name" to it.name
                )
            }
        }
    }

    fun putGuild(
        substitutor: Substitutor,
        guild: Guild,
        prefix: String = "",
        noneValue: String = "-",
    ): Substitutor = substitutor.apply {
        putAllLazy(
            mapOf(
                key(prefix, "guild_id") to { guild.id },
                key(prefix, "guild_name") to { guild.name },
                key(prefix, "guild_name_upper") to { guild.name.uppercase() },
                key(prefix, "guild_name_lower") to { guild.name.lowercase() },
                key(prefix, "guild_locale") to { guild.locale.name },
                key(prefix, "guild_member_count") to { guild.memberCount.toString() },
                key(prefix, "guild_boost_count") to { guild.boostCount.toString() },
                key(prefix, "guild_booster_count") to { guild.boostCount.toString() }, // compatibility alias
                key(prefix, "guild_icon_url") to { normalize(guild.iconUrl, noneValue) },
                key(prefix, "guild_banner_url") to { normalize(guild.bannerUrl, noneValue) },
                key(prefix, "guild_description") to { normalize(guild.description, noneValue) },
                key(prefix, "guild_owner_id") to { guild.owner?.id ?: "0" },
                key(prefix, "guild_owner_name") to { guild.owner?.effectiveName ?: noneValue },
                key(prefix, "guild_owner_mention") to { guild.owner?.asMention ?: noneValue },
                key(prefix, "guild_system_channel_id") to { guild.systemChannel?.id ?: "0" },
                key(prefix, "guild_system_channel_name") to { guild.systemChannel?.name ?: noneValue },
                key(prefix, "guild_system_channel_mention") to { guild.systemChannel?.asMention ?: noneValue },
                key(prefix, "guild_rules_channel_id") to { guild.rulesChannel?.id ?: "0" },
                key(prefix, "guild_rules_channel_name") to { guild.rulesChannel?.name ?: noneValue },
                key(prefix, "guild_rules_channel_mention") to { guild.rulesChannel?.asMention ?: noneValue },
            )
        )
    }

    fun putUser(
        substitutor: Substitutor,
        user: User,
        member: Member? = null,
        prefix: String = "",
        noneValue: String = "-",
    ): Substitutor = substitutor.apply {
        putAllLazy(
            mapOf(
                key(prefix, "user_id") to { user.id },
                key(prefix, "user_name") to { user.name },
                key(prefix, "user_name_upper") to { user.name.uppercase() },
                key(prefix, "user_name_lower") to { user.name.lowercase() },
                key(prefix, "user_global_name") to { normalize(user.globalName, noneValue) },
                key(prefix, "user_effective_name") to { member?.effectiveName ?: user.name },
                key(prefix, "user_mention") to { user.asMention },
                key(prefix, "user_avatar_url") to { normalize(user.effectiveAvatarUrl, noneValue) },
                key(prefix, "user_default_avatar_url") to { normalize(user.defaultAvatarUrl, noneValue) },
                key(prefix, "user_is_bot") to { user.isBot.toString() },
                key(prefix, "user_created_unix") to { user.timeCreated.toEpochSecond().toString() },
                key(prefix, "user_created_iso") to { user.timeCreated.toString() },
            )
        )
    }

    fun putMember(
        substitutor: Substitutor,
        user: User,
        member: Member?,
        prefix: String = "",
        noneValue: String = "-",
    ): Substitutor = substitutor.apply {
        putAllLazy(
            mapOf(
                key(prefix, "member_id") to { member?.id ?: user.id },
                key(prefix, "member_display_name") to { member?.effectiveName ?: user.name },
                key(prefix, "member_nickname") to { normalize(member?.nickname, noneValue) },
                key(prefix, "member_nickname_or_name") to { member?.nickname ?: member?.effectiveName ?: user.name },
                key(prefix, "member_avatar_url") to { normalize(member?.effectiveAvatarUrl ?: user.effectiveAvatarUrl, noneValue) },
                key(prefix, "member_roles_count") to { (member?.roles?.size ?: 0).toString() },
                key(prefix, "member_roles_mentions") to {
                    member?.roles?.joinToString(separator = " ") { it.asMention }?.ifBlank { noneValue } ?: noneValue
                },
                key(prefix, "member_roles_names") to {
                    member?.roles?.joinToString(separator = ", ") { it.name }?.ifBlank { noneValue } ?: noneValue
                },
                key(prefix, "member_joined_unix") to { member?.timeJoined?.toEpochSecond()?.toString() ?: "0" },
                key(prefix, "member_joined_iso") to { member?.timeJoined?.toString() ?: noneValue },
            )
        )
    }

    fun putEvent(
        substitutor: Substitutor,
        eventType: String,
        prefix: String = "",
        instant: Instant = Instant.now(),
    ): Substitutor = substitutor.apply {
        putAll(
            key(prefix, "event_type") to eventType,
            key(prefix, "event_timestamp_unix") to instant.epochSecond.toString(),
            key(prefix, "event_timestamp_ms") to instant.toEpochMilli().toString(),
            key(prefix, "event_timestamp_iso") to instant.toString(),
        )
    }

    fun putTextChannel(
        substitutor: Substitutor,
        channel: TextChannel?,
        prefix: String = "",
        baseKey: String = "channel",
        noneValue: String = "-",
    ): Substitutor = substitutor.apply {
        putAllLazy(
            mapOf(
                key(prefix, "${baseKey}_id") to { channel?.id ?: "0" },
                key(prefix, "${baseKey}_name") to { channel?.name ?: noneValue },
                key(prefix, "${baseKey}_mention") to { channel?.asMention ?: noneValue },
                key(prefix, "${baseKey}_type") to { channel?.type?.name ?: noneValue },
            )
        )
    }

    private fun key(prefix: String, key: String): String = if (prefix.isEmpty()) key else "${prefix}${key}"

    private fun normalize(value: String?, noneValue: String): String = value?.ifBlank { noneValue } ?: noneValue
}
