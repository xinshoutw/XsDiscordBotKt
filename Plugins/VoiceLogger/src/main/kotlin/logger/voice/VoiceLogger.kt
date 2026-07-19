package tw.xinshou.discord.plugin.logger.voice

import tw.xinshou.discord.core.i18n.MessageTemplate
import tw.xinshou.discord.core.placeholder.Substitutor
import tw.xinshou.discord.core.placeholder.withMember
import tw.xinshou.discord.core.placeholder.withUser
import tw.xinshou.discord.core.placeholder.withGuild
import tw.xinshou.discord.core.util.ComponentId
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.discord.plugin.logger.voice.Event.config
import tw.xinshou.discord.plugin.logger.voice.Event.placeholderLocalizer
import tw.xinshou.discord.plugin.logger.voice.Event.pluginDirectory
import tw.xinshou.discord.plugin.logger.voice.JsonManager.dataMap
import java.io.File
import java.util.stream.Collectors


internal object VoiceLogger {
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
            "modify_allow_menu" -> JsonManager.addAllowChannels(guild, event.channelIdLong, channelIds)
            "modify_block_menu" -> JsonManager.addBlockChannels(guild, event.channelIdLong, channelIds)
            else -> throw Exception("Unknown key ${event.componentId.removePrefix(config.componentPrefix)}")
        }

        val substitutor = Substitutor()
            .withUser(event.user)
            .withMember(event.member)
            .withGuild(event.guild)

        event.deferEdit().flatMap {
            event.hook.editOriginal(getSettingMenu(channelData, event.userLocale, substitutor))
        }.queue()
    }

    fun onChannelUpdateVoiceStatus(event: ChannelUpdateVoiceStatusEvent) {
        val locale =
            if (!event.guild.features.contains("COMMUNITY")) DiscordLocale.CHINESE_TAIWAN
            else event.guild.locale
        val channel: VoiceChannel = event.channel.asVoiceChannel()
        val oldStr = event.oldValue
        val newStr = event.newValue

        event.guild.retrieveAuditLogs()
            .limit(1)
            .type(ActionType.VOICE_CHANNEL_STATUS_UPDATE)
            .flatMap {
                event.guild.retrieveMemberById(it[0].userIdLong)
            }.map {
                val data = StatusEventData(event.guild.idLong, locale, channel, it, oldStr, newStr)
                if (newStr!!.isEmpty()) {
                    onChannelStatusDelete(event, data)
                } else if (oldStr!!.isEmpty()) {
                    onChannelStatusNew(event, data)
                } else {
                    onChannelStatusUpdate(event, data)
                }
            }.queue()
    }


    fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val locale =
            if (!event.guild.features.contains("COMMUNITY")) DiscordLocale.CHINESE_TAIWAN
            else event.guild.locale
        val member = event.member
        val channelJoin = event.channelJoined
        val channelLeft = event.channelLeft
        val data = VoiceEventData(event.guild.idLong, locale, member, channelJoin, channelLeft)

        if (channelJoin == null) {
            onChannelLeft(event, data)
        } else if (channelLeft == null) {
            onChannelJoin(event, data)
        } else {
            onChannelSwitch(event, data)
        }
    }

    private fun onChannelStatusNew(event: ChannelUpdateVoiceStatusEvent, data: StatusEventData) {
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) -> data.channel in value.getCurrentDetectChannels() }
            .map { (key, _) -> key }
        if (listenChannelIds.isEmpty()) return

        data.member?.let { member ->
            val substitutor = Substitutor().withUser(member.user).withMember(member).putAll(
                "vl_channel_mention" to data.channel.asMention,
                "vl_channel_url" to data.channel.jumpUrl,
                "vl_status_after" to data.newStr!!,
            )
            data.channel.parentCategory?.let { substitutor.put("vl_category_mention", it.asMention) }
            sendListenChannel("on-status-new", event.guild, listenChannelIds, substitutor)
        }
    }

    private fun createSelButton(event: ButtonInteractionEvent, key: String) {
        val substitutor = Substitutor().withUser(event.user).withMember(event.member).withGuild(event.guild)
        event.editMessage(
            MessageEditData.fromCreateData(
                messageTemplate.buildCreate(key, event.userLocale, substitutor).build()
            )
        ).queue()
    }

    private fun onToggleButton(event: ButtonInteractionEvent) {
        val channelData = JsonManager.toggle(event.guild!!, event.channel.idLong)
        val substitutor = Substitutor().withUser(event.user).withMember(event.member).withGuild(event.guild)
        event.deferEdit().flatMap {
            event.hook.editOriginal(getSettingMenu(channelData, event.userLocale, substitutor))
        }.queue()
    }

    private fun onDeleteButton(event: ButtonInteractionEvent) {
        JsonManager.delete(event.guild!!.idLong, event.channel.idLong)
        val substitutor = Substitutor().withUser(event.user).withMember(event.member).withGuild(event.guild)
        event.editMessage(
            MessageEditData.fromCreateData(
                messageTemplate.buildCreate("delete", event.userLocale, substitutor).build()
            )
        ).queue()
    }

    private fun onChannelStatusUpdate(event: ChannelUpdateVoiceStatusEvent, data: StatusEventData) {
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) -> data.channel in value.getCurrentDetectChannels() }
            .map { (key, _) -> key }
        if (listenChannelIds.isEmpty()) return

        data.member?.let { member ->
            val substitutor = Substitutor().withUser(member.user).withMember(member).putAll(
                "vl_channel_mention" to data.channel.asMention,
                "vl_channel_url" to data.channel.jumpUrl,
                "vl_status_before" to data.oldStr!!,
                "vl_status_after" to data.newStr!!,
            )
            data.channel.parentCategory?.let { substitutor.put("vl_category_mention", it.asMention) }
            sendListenChannel("on-status-update", event.guild, listenChannelIds, substitutor)
        }
    }

    private fun onChannelStatusDelete(event: ChannelUpdateVoiceStatusEvent, data: StatusEventData) {
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) -> data.channel in value.getCurrentDetectChannels() }
            .map { (key, _) -> key }
        if (listenChannelIds.isEmpty()) return

        data.member?.let { member ->
            val substitutor = Substitutor().withUser(member.user).withMember(member).putAll(
                "vl_channel_mention" to data.channel.asMention,
                "vl_channel_url" to data.channel.jumpUrl,
                "vl_status_before" to data.oldStr!!,
            )
            data.channel.parentCategory?.let { substitutor.put("vl_category_mention", it.asMention) }
            sendListenChannel("on-status-delete", event.guild, listenChannelIds, substitutor)
        }
    }

    private fun onChannelJoin(event: GuildVoiceUpdateEvent, data: VoiceEventData) {
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) -> data.channelJoin!! in value.getCurrentDetectChannels() }
            .map { (key, _) -> key }
        if (listenChannelIds.isEmpty()) return

        val substitutor = Substitutor().withUser(data.member.user).withMember(data.member).putAll(
            "vl_channel_join_mention" to data.channelJoin!!.asMention,
            "vl_channel_join_name" to data.channelJoin.name,
            "vl_channel_join_url" to data.channelJoin.jumpUrl,
        )
        data.channelJoin.parentCategory?.let { substitutor.put("vl_category_join_mention", it.asMention) }
        sendListenChannel("on-channel-join", event.guild, listenChannelIds, substitutor)
    }

    private fun onChannelSwitch(event: GuildVoiceUpdateEvent, data: VoiceEventData) {
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) ->
                (data.channelJoin!! in value.getCurrentDetectChannels()) or
                        (data.channelLeft!! in value.getCurrentDetectChannels())
            }
            .map { (key, _) -> key }
        if (listenChannelIds.isEmpty()) return

        val substitutor = Substitutor().withUser(data.member.user).withMember(data.member).putAll(
            "vl_channel_join_url" to data.channelJoin!!.jumpUrl,
            "vl_channel_join_name" to data.channelJoin.name,
            "vl_channel_left_mention" to data.channelLeft!!.asMention,
            "vl_channel_left_name" to data.channelLeft.name,
            "vl_channel_left_url" to data.channelLeft.jumpUrl,
        )
        data.channelJoin.parentCategory?.let { substitutor.put("vl_category_join_mention", it.asMention) }
        data.channelLeft.parentCategory?.let { substitutor.put("vl_category_left_mention", it.asMention) }
        sendListenChannel("on-channel-switch", event.guild, listenChannelIds, substitutor)
    }

    private fun onChannelLeft(event: GuildVoiceUpdateEvent, data: VoiceEventData) {
        val listenChannelIds: List<Long> = dataMap.entries
            .filter { (_, value) -> data.channelLeft!! in value.getCurrentDetectChannels() }
            .map { (key, _) -> key }
        if (listenChannelIds.isEmpty()) return

        val substitutor = Substitutor().withUser(data.member.user).withMember(data.member).putAll(
            "vl_channel_left_mention" to data.channelLeft!!.asMention,
            "vl_channel_left_name" to data.channelLeft.name,
            "vl_channel_left_url" to data.channelLeft.jumpUrl,
        )
        data.channelLeft.parentCategory?.let { substitutor.put("vl_category_left_mention", it.asMention) }
        sendListenChannel("on-channel-left", event.guild, listenChannelIds, substitutor)
    }

    private fun sendListenChannel(key: String, guild: Guild, listenChannelId: List<Long>, substitutor: Substitutor) {
        val message = messageTemplate.buildCreate(key, guild.locale, substitutor).build()
        listenChannelId.forEach {
            val listenChannel = guild.getGuildChannelById(it) ?: return
            when (listenChannel) {
                is TextChannel -> listenChannel.sendMessage(message).queue()
                is VoiceChannel -> listenChannel.sendMessage(message).queue()
                else -> throw Exception("Unknown channel type")
            }
        }
    }

    private data class StatusEventData(
        val guildId: Long, val locale: DiscordLocale, val channel: VoiceChannel,
        val member: Member?, val oldStr: String?, val newStr: String?,
    )

    private data class VoiceEventData(
        val guildId: Long, val locale: DiscordLocale, val member: Member,
        val channelJoin: AudioChannel?, val channelLeft: AudioChannel?,
    )

    private fun getSettingMenu(
        channelData: ChannelData, locale: DiscordLocale, substitutor: Substitutor
    ): MessageEditData {
        val allowListFormat = placeholderLocalizer.get("allowListFormat", locale)
        val blockListFormat = placeholderLocalizer.get("blockListFormat", locale)

        val allowString = StringBuilder().apply {
            if (channelData.getAllow().isEmpty()) {
                append(substitutor.parse(placeholderLocalizer.get("empty", locale)))
            } else {
                channelData.getAllow().map { it.toString() }.map {
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
                channelData.getBlock().map { it.toString() }.map {
                    substitutor.parse(
                        blockListFormat
                            .replace("%blocklist_channel_mention%", "<#${it}>")
                            .replace("%blocklist_channel_id%", it)
                    )
                }.forEach { append(it) }
            }
        }.toString()

        substitutor.putAll(
            "vl_channel_mode" to if (channelData.getChannelMode()) "ALLOW" else "BLOCK",
            "vl_allow_list_format" to allowString,
            "vl_block_list_format" to blockString
        )

        return MessageEditData.fromCreateData(
            messageTemplate.buildCreate("voice-logger@setting", locale, substitutor).build()
        )
    }
}
