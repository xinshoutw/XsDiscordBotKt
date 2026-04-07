package tw.xinshou.discord.plugin.logger.chat

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import core.placeholder.withCommand
import core.placeholder.withGuild
import core.placeholder.withMember
import core.placeholder.withUser
import core.util.ComponentId
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.discord.plugin.logger.chat.Event.config
import tw.xinshou.discord.plugin.logger.chat.Event.placeholderLocalizer
import tw.xinshou.discord.plugin.logger.chat.Event.pluginConfig
import tw.xinshou.discord.plugin.logger.chat.Event.pluginDirectory
import tw.xinshou.discord.plugin.logger.chat.JsonManager.dataMap
import java.io.File
import java.util.stream.Collectors


internal object ChatLogger {
    private val componentId = ComponentId(
        prefix = config.componentPrefix,
        idKeys = mapOf(
            "action" to ComponentId.FieldType.STRING,
        )
    )

    private var messageTemplate = MessageTemplate(
        langDir = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdPrefix = config.componentPrefix,
    )

    internal fun reload() {
        messageTemplate = MessageTemplate(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdPrefix = config.componentPrefix,
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withGuild(event.guild)
            .withCommand(event)

        event.hook.editOriginal(
            getSettingMenu(
                dataMap.computeIfAbsent(event.channelIdLong) { ChannelData(event.guild!!) },
                event.userLocale,
                substitutor
            )
        ).queue()
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val idMap = componentId.parse(event.componentId)
        when (idMap["action"]) {
            "toggle_btn" -> onToggleButton(event)
            "modify_allow_btn" -> createSelButton(event, "modify-allow")
            "modify_block_btn" -> createSelButton(event, "modify-block")
            "delete_btn" -> onDeleteButton(event)
        }
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val guild = event.guild!!
        val channelIds = event.values
            .stream()
            .map { obj: IMentionable -> obj.idLong }
            .collect(Collectors.toSet())
        val idMap = componentId.parse(event.componentId)
        val channelData = when (idMap["action"]) {
            "modify_allow_menu" -> {
                JsonManager.addAllowChannels(
                    guild = guild,
                    listenChannelId = event.channelIdLong,
                    detectedChannelIds = channelIds
                )
            }

            "modify_block_menu" -> {
                JsonManager.addBlockChannels(
                    guild = guild,
                    listenChannelId = event.channelIdLong,
                    detectedChannelIds = channelIds
                )
            }

            else -> throw Exception("Unknown key ${event.componentId.removePrefix(config.componentPrefix)}")
        }

        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withGuild(event.guild)

        event.deferEdit().flatMap {
            event.hook.editOriginal(
                getSettingMenu(channelData, event.userLocale, substitutor)
            )
        }.queue()
    }

    fun onMessageReceived(event: MessageReceivedEvent) {
        if (!isListenable(event.channel.idLong)) return

        val messageContent = getMessageContent(event.message)
        DbManager.receiveMessage(
            event.guild.id,
            event.channel.idLong,
            event.messageIdLong,
            event.author.idLong,
            messageContent
        )
    }

    fun onMessageUpdate(event: MessageUpdateEvent) {
        val channel = event.guildChannel
        if (!isListenable(channel.idLong)) return
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) -> channel in value.getCurrentDetectChannels() }
            .map { (key, _) -> key }

        try {
            val newMessage = getMessageContent(event.message)
            val (oldMessage, _, updateCount) = DbManager.updateMessage(
                event.guild.id,
                channel.idLong,
                event.messageIdLong,
                newMessage
            )

            if (listenChannelIds.isEmpty()) return
            val substitutor = Substitutor().apply {
                event.member?.let {
                    withUser(it.user)
                    withMember(it)
                }
                putAll(
                    "cl_msg_after_url" to event.message.jumpUrl,
                    "cl_channel_mention" to channel.asMention,
                    "cl_change_count" to updateCount.toString(),
                    "cl_msg_before" to oldMessage,
                    "cl_msg_after" to newMessage
                )
            }

            channel.asStandardGuildChannel().parentCategory?.let { category ->
                substitutor.put("cl_category_mention", category.asMention)
            }

            sendListenChannel("on-msg-update", event.guild, listenChannelIds, substitutor)
        } catch (_: MessageNotFound) {
            return
        }
    }

    fun onMessageDelete(event: MessageDeleteEvent) {
        val channel = event.guildChannel
        if (!isListenable(channel.idLong)) return
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) -> channel in value.getCurrentDetectChannels() }
            .map { (key, _) -> key }

        try {
            val (oldMessage: String, userId: Long, updateCount: Int) = DbManager.deleteMessage(
                event.guild.id,
                channel.idLong,
                event.messageIdLong,
            )

            if (listenChannelIds.isEmpty()) return

            event.guild.retrieveMemberById(userId).queue { member ->
                val substitutor = Substitutor()
                    .withUser(member.user)
                    .withMember(member)
                    .putAll(
                        "cl_channel_mention" to channel.asMention,
                        "cl_change_count" to updateCount.toString(),
                        "cl_msg_before" to oldMessage,
                    )

                channel.asStandardGuildChannel().parentCategory?.let { category ->
                    substitutor.put("cl_category_mention", category.asMention)
                }

                sendListenChannel("on-msg-delete", event.guild, listenChannelIds, substitutor)
            }
        } catch (_: MessageNotFound) {
            return
        } catch (e: ErrorResponseException) {
            when (e.errorCode) {
                10007 -> {
                    sendListenChannel(
                        "on-msg-delete",
                        event.guild,
                        listenChannelIds,
                        Substitutor()
                    )
                    return
                }
            }
        }
    }


    private fun createSelButton(event: ButtonInteractionEvent, key: String) {
        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withGuild(event.guild)

        event.editMessage(
            MessageEditData.fromCreateData(
                messageTemplate.buildCreate(key, event.userLocale, substitutor).build()
            )
        ).queue()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        if (pluginConfig.logAll) return
        DbManager.deleteDatabase(event.guild.id)
    }


    private fun onToggleButton(event: ButtonInteractionEvent) {
        val channelData = JsonManager.toggle(event.guild!!, event.channel.idLong)

        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withGuild(event.guild)

        event.deferEdit().flatMap {
            event.hook.editOriginal(
                getSettingMenu(channelData, event.userLocale, substitutor)
            )
        }.queue()
    }

    private fun onDeleteButton(event: ButtonInteractionEvent) {
        JsonManager.delete(event.guild!!.idLong, event.channel.idLong)

        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withGuild(event.guild)

        event.editMessage(
            MessageEditData.fromCreateData(
                messageTemplate.buildCreate("delete", event.userLocale, substitutor).build()
            )
        ).queue()
    }

    private fun sendListenChannel(key: String, guild: Guild, listenChannelId: List<Long>, substitutor: Substitutor) {
        val message = messageTemplate.buildCreate(key, guild.locale, substitutor).build()

        listenChannelId.forEach {
            val listenChannel = guild.getGuildChannelById(it)
            if (listenChannel == null) {
                DbManager.markChannelAsUnavailable(it)
                return
            }

            when (listenChannel) {
                is TextChannel -> listenChannel.sendMessage(message).queue()
                is VoiceChannel -> listenChannel.sendMessage(message).queue()
                else -> throw Exception("Unknown channel type")
            }
        }
    }


    private fun getMessageContent(message: Message): String {
        if (message.embeds.isEmpty()) {
            return message.contentRaw
        }

        return StringBuilder().apply {
            for (embed in message.embeds) {
                append(embed.author?.let { "${it.name}\n" } ?: "")
                append("${embed.title}\n\n")
                append(embed.description)

                for (field in embed.fields) {
                    append("${field.name}\n")
                    append("${field.value}\n\n")
                }
            }
        }.toString()
    }


    private fun getSettingMenu(
        channelData: ChannelData,
        locale: DiscordLocale,
        substitutor: Substitutor
    ): MessageEditData {
        val allowListFormat = placeholderLocalizer.get("allowListFormat", locale)
        val blockListFormat = placeholderLocalizer.get("blockListFormat", locale)

        val allowString = StringBuilder().apply {
            if (channelData.getAllow().isEmpty()) {
                append(substitutor.parse(placeholderLocalizer.get("empty", locale)))
            } else {
                channelData.getAllow()
                    .map { it.toString() }
                    .map {
                        substitutor.parse(
                            allowListFormat
                                .replace("%allowlist_channel_mention%", "<#${it}>")
                                .replace("%allowlist_channel_id%", it)
                        )
                    }.forEach { append(it) }
            }
        }.toString()

        val blockString = StringBuilder().apply {
            if (channelData.getBlock().isEmpty()) {
                append(substitutor.parse(placeholderLocalizer.get("empty", locale)))
            } else {
                channelData.getBlock()
                    .map { it.toString() }
                    .map {
                        substitutor.parse(
                            blockListFormat
                                .replace("%blocklist_channel_mention%", "<#${it}>")
                                .replace("%blocklist_channel_id%", it)
                        )
                    }.forEach { append(it) }
            }
        }.toString()

        substitutor.apply {
            putAll(
                "cl_channel_mode" to if (channelData.getChannelMode()) "ALLOW" else "BLOCK",
                "cl_allow_list_format" to allowString,
                "cl_block_list_format" to blockString
            )
        }

        return MessageEditData.fromCreateData(
            messageTemplate.buildCreate("chat-logger@setting", locale, substitutor).build()
        )
    }


    private fun isListenable(channelId: Long): Boolean {
        return pluginConfig.logAll ||
                DbManager.isChannelInTableCache(channelId)
    }
}
