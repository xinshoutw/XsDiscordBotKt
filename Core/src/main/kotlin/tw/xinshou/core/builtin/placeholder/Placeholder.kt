package tw.xinshou.core.builtin.placeholder

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectInteraction
import tw.xinshou.core.base.BotLoader.jdaBot

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
}