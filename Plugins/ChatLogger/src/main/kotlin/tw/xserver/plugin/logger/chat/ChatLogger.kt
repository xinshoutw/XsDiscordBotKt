package tw.xserver.plugin.logger.chat

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
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.plugin.creator.message.MessageCreator
import tw.xserver.plugin.logger.chat.Event.COMPONENT_PREFIX
import tw.xserver.plugin.logger.chat.Event.PLUGIN_DIR_FILE
import tw.xserver.plugin.logger.chat.Event.config
import tw.xserver.plugin.logger.chat.JsonManager.dataMap
import tw.xserver.plugin.logger.chat.lang.PlaceholderLocalizations
import java.io.File
import java.util.stream.Collectors


internal object ChatLogger {
    private val creator = MessageCreator(File(PLUGIN_DIR_FILE, "lang"), DiscordLocale.CHINESE_TAIWAN, COMPONENT_PREFIX)

    fun setting(event: SlashCommandInteractionEvent) = event.hook.editOriginal(
        getSettingMenu(
            dataMap.computeIfAbsent(event.channelIdLong) { ChannelData(event.guild!!) },
            event.userLocale,
            Placeholder.getSubstitutor(event)
        )
    ).queue()

    fun onToggle(event: ButtonInteractionEvent) {
        // update
        val channelData = JsonManager.toggle(event.guild!!, event.channel.idLong)

        // reply
        event.hook.editOriginal(
            getSettingMenu(
                channelData,
                event.userLocale,
                Placeholder.getSubstitutor(event)
            )
        ).queue()
        event.deferEdit().queue()
    }

    fun onDelete(event: ButtonInteractionEvent) {
        // update
        JsonManager.delete(event.guild!!.idLong, event.channel.idLong)

        // reply
        event.editMessage(
            MessageEditData.fromCreateData(
                creator.getCreateBuilder(
                    "delete",
                    event.userLocale,
                    Placeholder.getSubstitutor(event)
                ).build()
            )
        ).queue()
    }

    fun onSelect(event: EntitySelectInteractionEvent) {
        // update
        val guild = event.guild!!
        val channelIds = event.values
            .stream()
            .map { obj: IMentionable -> obj.idLong }
            .collect(Collectors.toList())
        val channelData = when (event.componentId.removePrefix(COMPONENT_PREFIX)) {
            "modify_allow" -> {
                JsonManager.addAllowChannels(
                    guild = guild,
                    listenChannelId = event.channelIdLong,
                    detectedChannelIds = channelIds
                )
            }

            "modify_block" -> {
                JsonManager.addBlockChannels(
                    guild = guild,
                    listenChannelId = event.channelIdLong,
                    detectedChannelIds = channelIds
                )
            }

            else -> throw Exception("Unknown key ${event.componentId.removePrefix(COMPONENT_PREFIX)}")
        }

        // reply
        event.hook.editOriginal(
            getSettingMenu(
                channelData,
                event.userLocale,
                Placeholder.getSubstitutor(event)
            )
        ).queue()
        event.deferEdit().queue()
    }

    fun createSel(event: ButtonInteractionEvent, componentId: String) {
        event.editMessage(
            MessageEditData.fromCreateData(
                creator.getCreateBuilder(
                    componentId,
                    event.userLocale, Placeholder.getSubstitutor(event)
                ).build()
            )
        ).queue()
    }

    fun receiveMessage(event: MessageReceivedEvent) {
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

    fun updateMessage(event: MessageUpdateEvent) {
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

            val substitutor = Placeholder.getSubstitutor(event.member!!).putAll(
                "cl_msg_after_url" to event.message.jumpUrl,
                "cl_category_mention" to channel.asTextChannel().parentCategory!!.asMention,
                "cl_channel_mention" to channel.asMention,
                "cl_change_count" to updateCount.toString(),
                "cl_msg_before" to oldMessage,
                "cl_msg_after" to newMessage
            )

            sendListenChannel(
                "on-msg-update",
                event.guild,
                listenChannelIds,
                substitutor
            )
        } catch (_: MessageNotFound) {
            return
        }
    }

    fun deleteMessage(event: MessageDeleteEvent) {
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
                val substitutor = Placeholder.getSubstitutor(member).putAll(
                    "cl_category_mention" to channel.asTextChannel().parentCategory!!.asMention,
                    "cl_channel_mention" to channel.asMention,
                    "cl_change_count" to updateCount.toString(),
                    "cl_msg_before" to oldMessage,
                )
                sendListenChannel(
                    "on-msg-delete",
                    event.guild,
                    listenChannelIds,
                    substitutor
                )
            }
        } catch (_: MessageNotFound) {
            return
        } catch (e: ErrorResponseException) {
            when (e.errorCode) {
                // Unknown Member
                10007 -> {
                    sendListenChannel(
                        "on-msg-delete",
                        event.guild,
                        listenChannelIds,
                        Placeholder.globalPlaceholder
                    )
                    return
                }
            }
        }
    }

    internal fun onGuildLeave(event: GuildLeaveEvent) {
        if (config.logAll) return
        DbManager.deleteDatabase(event.guild.id)
    }

    private fun sendListenChannel(key: String, guild: Guild, listenChannelId: List<Long>, substitutor: Substitutor) {
        val message = creator.getCreateBuilder(key, guild.locale, substitutor).build()

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
            // It's a default message
            return message.contentRaw
        }

        // It's an embed message
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
        val allowListFormat = PlaceholderLocalizations.allowListFormat[locale]
        val blockListFormat = PlaceholderLocalizations.blockListFormat[locale]

        val allowString = StringBuilder().apply {
            if (!channelData.getAllowArray().isEmpty) channelData.getAllowArray().map { it.asString }
                .forEach { detectedChannelId ->
                    append(
                        substitutor.parse(
                            allowListFormat
                                .replace("%allowlist_channel_mention%", "<#${detectedChannelId}>")
                                .replace("%allowlist_channel_id%", detectedChannelId)
                        )
                    )
                } else {
                append(substitutor.parse(PlaceholderLocalizations.empty[locale]))
            }
        }.toString()

        val blockString = StringBuilder().apply {
            if (!channelData.getBlockArray().isEmpty) channelData.getBlockArray().map { it.asString }
                .forEach { detectedChannelId ->
                    append(
                        substitutor.parse(
                            blockListFormat
                                .replace("%blocklist_channel_mention%", "<#${detectedChannelId}>")
                                .replace("%blocklist_channel_id%", detectedChannelId)
                        )
                    )
                } else {
                append(substitutor.parse(PlaceholderLocalizations.empty[locale]))
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
            creator.getCreateBuilder(
                "chat-logger@setting",
                locale,
                substitutor
            ).build()
        )
    }

    private fun isListenable(channelId: Long): Boolean {
        return config.logAll ||
                DbManager.isChannelInTableCache(channelId)
    }
}