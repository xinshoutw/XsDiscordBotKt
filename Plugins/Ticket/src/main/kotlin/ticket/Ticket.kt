package tw.xinshou.discord.plugin.ticket

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.Permission.ADMINISTRATOR
import net.dv8tion.jda.api.Permission.CREATE_PUBLIC_THREADS
import net.dv8tion.jda.api.Permission.MANAGE_WEBHOOKS
import net.dv8tion.jda.api.Permission.MESSAGE_HISTORY
import net.dv8tion.jda.api.Permission.MESSAGE_SEND
import net.dv8tion.jda.api.Permission.MESSAGE_SEND_IN_THREADS
import net.dv8tion.jda.api.Permission.VIEW_CHANNEL
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import tw.xinshou.discord.core.builtin.messagecreator.v2.MessageCreator
import tw.xinshou.discord.core.builtin.messagecreator.modal.ModalCreator
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.core.util.ComponentIdManager
import tw.xinshou.discord.core.util.FieldType
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.ticket.Event.componentPrefix
import tw.xinshou.discord.plugin.ticket.Event.pluginDirectory
import tw.xinshou.discord.plugin.ticket.create.StepManager
import tw.xinshou.discord.plugin.ticket.json.serializer.JsonDataClass
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletionException

internal object Ticket {
    private const val threadNameMaxLength = 100
    private val threadTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")

    val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()
    val jsonGuildManager = JsonGuildFileManager<JsonDataClass>(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = mutableMapOf()
    )

    val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "sub_action" to FieldType.STRING,
            "color_index" to FieldType.STRING,
            "user_id" to FieldType.LONG_HEX, // lock / unlock
            "msg_id" to FieldType.STRING,
            "btn_index" to FieldType.STRING,
        )
    )
    var messageCreator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager
    )
    var modalCreator = ModalCreator(
        langDirFile = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager
    )

    internal fun reload() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager
        )

        modalCreator = ModalCreator(
            langDirFile = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        StepManager.onSlashCommandInteraction(event)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        GlobalUtil.checkComponentIdPrefix(event, componentPrefix)
        val idMap = componentIdManager.parse(event.componentId)
        val guild = event.guild!!

        when (idMap["action"]) {
            // Trying to create or add ticket buttons
            "create" -> {
                StepManager.onButtonInteraction(event, idMap)
            }

            // Run ticket action
            "press" -> {
                val reason = jsonGuildManager
                    .get(guild.idLong)
                    .data
                    .get(event.messageId)
                    ?.get((idMap["btn_index"] as String).toInt())
                    ?.reasonTitle
                    ?: { throw IllegalStateException("Cannot find data.") }()

                event.replyModal(
                    modalCreator.getModalBuilder(
                        "press-ticket",
                        event.userLocale,
                        substitutor = Placeholder.get(event).putAll(
                            mapOf(
                                "tt@msg-id" to event.messageId,
                                "tt@btn-index" to idMap["btn_index"] as String,
                                "tt@reason" to reason
                            )
                        )
                    ).build()
                ).queue()
            }

            "lock" -> {
                val channel = event.guildChannel.asTextChannel()
                guild.retrieveMemberById(idMap["user_id"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).deny(VIEW_CHANNEL) }
                    .flatMap {
                        event.editComponents(
                            buildTicketActionRow(
                                isLocked = true,
                                userId = idMap["user_id"] as Long,
                                msgId = idMap["msg_id"] as String,
                                btnIndex = idMap["btn_index"] as String,
                            )
                        )
                    }.queue()
            }

            "unlock" -> {
                val channel = event.guildChannel.asTextChannel()

                guild.retrieveMemberById(idMap["user_id"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).grant(VIEW_CHANNEL) }
                    .flatMap {
                        event.editComponents(
                            buildTicketActionRow(
                                isLocked = false,
                                userId = idMap["user_id"] as Long,
                                msgId = idMap["msg_id"] as String,
                                btnIndex = idMap["btn_index"] as String,
                            )
                        )
                    }.queue()
            }

            "delete" -> {
                val member = event.member ?: return event.deferEdit().queue()
                if (hasTicketAdminPermission(member, idMap)) {
                    event.deferEdit().flatMap {
                        event.guildChannel.asTextChannel().delete()
                    }.queue()
                } else {
                    event.deferEdit().queue()
                }
            }

            "save" -> {
                val member = event.member ?: return event.deferEdit().queue()
                if (!hasTicketAdminPermission(member, idMap)) {
                    event.reply("僅管理員可使用此功能").setEphemeral(true).queue()
                    return
                }

                val selectMenu = EntitySelectMenu.create(
                    componentIdManager.build(
                        mapOf(
                            "action" to "save",
                            "sub_action" to "select-channel",
                            "user_id" to idMap["user_id"] as Long,
                            "msg_id" to idMap["msg_id"] as String,
                            "btn_index" to idMap["btn_index"] as String,
                        )
                    ),
                    EntitySelectMenu.SelectTarget.CHANNEL
                )
                    .setRequiredRange(1, 1)
                    .setPlaceholder("請選擇要儲存對話的文字頻道")
                    .setChannelTypes(ChannelType.TEXT)
                    .build()

                event.reply("請選擇要儲存對話的文字頻道")
                    .setEphemeral(true)
                    .addComponents(ActionRow.of(selectMenu))
                    .queue()
            }
        }
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["action"]) {
            "create" -> StepManager.onEntitySelectInteraction(event)
            "save" -> onSaveContentSelect(event, idMap)
        }
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        StepManager.onGuildLeave(event)
    }

    fun onModalInteraction(event: ModalInteractionEvent) {
        val idMap = componentIdManager.parse(event.modalId)

        if (idMap["action"] == "create") {
            StepManager.onModalInteraction(event, idMap)
            return
        }

        if (idMap["action"] != "submit") return
        event.deferReply(true).queue()

        val guild = event.guild!!
        val jsonData = jsonGuildManager
            .get(guild.idLong)
            .data
            .get((idMap["msg_id"] as String))!!
            .get((idMap["btn_index"] as String).toInt())

        val reason = event.getValue("reason")!!.asString
        val roleIds = jsonData.adminIds
        val categoryId = jsonData.categoryId
        val category: Category? = if (categoryId == 0L) {
            event.guildChannel.asTextChannel().parentCategory
        } else {
            guild.getCategoryById(categoryId)
        }

        if (category == null) {
            event.reply("錯誤 (無法取得目錄)").setEphemeral(true).queue()
            return
        }

        val builder = StringBuilder()
        category.createTextChannel(event.user.name).apply {
            addPermissionOverride(
                guild.publicRole,
                Permission.getRaw(),
                VIEW_CHANNEL.rawValue
            )
            addMemberPermissionOverride(
                event.user.idLong,
                VIEW_CHANNEL.rawValue,
                Permission.getRaw()
            )

            for (roleId in roleIds) {
                addRolePermissionOverride(roleId, VIEW_CHANNEL.rawValue, Permission.getRaw())
                builder.append("<@&").append(roleId).append("> ")
            }
            builder.append("\n\n").append(reason)
        }.flatMap {
            event.hook.sendMessage("請到此頻道 <#${it.id}> 並等待人員回覆繼續!").queue()

            it.sendMessage(builder.toString()).addComponents(
                buildTicketActionRow(
                    isLocked = false,
                    userId = event.user.idLong,
                    msgId = idMap["msg_id"] as String,
                    btnIndex = idMap["btn_index"] as String,
                )
            )
        }.queue()
    }

    private fun onSaveContentSelect(event: EntitySelectInteractionEvent, idMap: Map<String, Any>) {
        if (idMap["sub_action"] != "select-channel") {
            event.deferEdit().queue()
            return
        }

        val member = event.member ?: run {
            event.reply("錯誤 (無法取得成員資訊)").setEphemeral(true).queue()
            return
        }
        if (!hasTicketAdminPermission(member, idMap)) {
            event.reply("僅管理員可使用此功能").setEphemeral(true).queue()
            return
        }

        val sourceChannel = event.guildChannel as? TextChannel ?: run {
            event.reply("錯誤 (目前頻道不是文字頻道)").setEphemeral(true).queue()
            return
        }
        val targetChannel = event.mentions.getChannels(TextChannel::class.java).firstOrNull() ?: run {
            event.reply("請選擇一個文字頻道").setEphemeral(true).queue()
            return
        }

        val selfMember = event.guild!!.selfMember
        if (!selfMember.hasPermission(sourceChannel, VIEW_CHANNEL, MESSAGE_HISTORY)) {
            event.reply("機器人缺少讀取 Ticket 訊息歷史的權限").setEphemeral(true).queue()
            return
        }

        if (!selfMember.hasPermission(
                targetChannel,
                VIEW_CHANNEL,
                MESSAGE_SEND,
                MESSAGE_SEND_IN_THREADS,
                CREATE_PUBLIC_THREADS,
                MANAGE_WEBHOOKS
            )
        ) {
            event.reply("機器人缺少目標頻道權限 (VIEW_CHANNEL / MESSAGE_SEND / MESSAGE_SEND_IN_THREADS / CREATE_PUBLIC_THREADS / MANAGE_WEBHOOKS)")
                .setEphemeral(true)
                .queue()
            return
        }

        event.deferReply(true).queue { hook ->
            hook.editOriginal("存檔中...").queue()

            archiveTicketContent(
                sourceChannel = sourceChannel,
                targetChannel = targetChannel,
                onSuccess = { thread ->
                    hook.editOriginal("存檔完成，已建立 Thread：<#${thread.id}>").queue()
                },
                onError = { throwable ->
                    hook.editOriginal("存檔失敗：${toReadableError(throwable)}").queue()
                }
            )
        }
    }

    private fun archiveTicketContent(
        sourceChannel: TextChannel,
        targetChannel: TextChannel,
        onSuccess: (ThreadChannel) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        targetChannel.createThreadChannel(
            buildArchiveThreadName(sourceChannel.name)
        ).queue({ thread ->
            targetChannel.createWebhook("ticket-save-content")
                .queue({ webhook ->
                    sourceChannel.iterableHistory
                        .cache(false)
                        .takeAsync(Int.MAX_VALUE)
                        .whenComplete { messages, throwable ->
                            if (throwable != null) {
                                safeDeleteWebhook(webhook)
                                onError(unwrapThrowable(throwable))
                                return@whenComplete
                            }

                            val archivedMessages = (messages ?: emptyList()).asReversed()
                            forwardMessagesWithWebhook(
                                webhook = webhook,
                                thread = thread,
                                messages = archivedMessages,
                                onComplete = {
                                    safeDeleteWebhook(webhook)
                                    onSuccess(thread)
                                },
                                onError = { forwardError ->
                                    safeDeleteWebhook(webhook)
                                    onError(forwardError)
                                }
                            )
                        }
                }, onError)
        }, onError)
    }

    private fun forwardMessagesWithWebhook(
        webhook: Webhook,
        thread: ThreadChannel,
        messages: List<Message>,
        index: Int = 0,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        if (index >= messages.size) {
            onComplete()
            return
        }

        val message = messages[index]
        if (!shouldForwardMessage(message)) {
            forwardMessagesWithWebhook(
                webhook = webhook,
                thread = thread,
                messages = messages,
                index = index + 1,
                onComplete = onComplete,
                onError = onError
            )
            return
        }

        webhook.sendMessage(buildForwardMessageData(message))
            .setThread(thread)
            .setUsername(message.member?.effectiveName ?: message.author.name)
            .setAvatarUrl(message.member?.effectiveAvatarUrl ?: message.author.effectiveAvatarUrl)
            .queue(
                {
                    forwardMessagesWithWebhook(
                        webhook = webhook,
                        thread = thread,
                        messages = messages,
                        index = index + 1,
                        onComplete = onComplete,
                        onError = onError
                    )
                },
                onError
            )
    }

    private fun buildForwardMessageData(message: Message) = MessageCreateBuilder().apply {
        if (message.contentRaw.isNotEmpty()) {
            setContent(message.contentRaw)
        }

        if (message.attachments.isNotEmpty()) {
            setFiles(
                message.attachments.map { attachment ->
                    attachment.proxy.downloadAsFileUpload(attachment.fileName)
                }
            )
        }
    }.build()

    private fun shouldForwardMessage(message: Message): Boolean {
        return message.contentRaw.isNotEmpty() || message.attachments.isNotEmpty()
    }

    private fun buildArchiveThreadName(ticketName: String): String {
        val timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(threadTimeFormatter)
        val suffix = "-$timestamp"
        val maxTicketNameLength = threadNameMaxLength - suffix.length
        val trimmedTicketName = if (ticketName.length > maxTicketNameLength) {
            ticketName.substring(0, maxTicketNameLength)
        } else {
            ticketName
        }

        return "$trimmedTicketName$suffix"
    }

    private fun buildTicketActionRow(
        isLocked: Boolean,
        userId: Long,
        msgId: String,
        btnIndex: String,
    ): ActionRow {
        val lockButton = if (isLocked) {
            Button.of(
                ButtonStyle.SUCCESS,
                buildTicketActionId("unlock", userId, msgId, btnIndex),
                "開啟",
                Emoji.fromUnicode("🔓")
            )
        } else {
            Button.of(
                ButtonStyle.SECONDARY,
                buildTicketActionId("lock", userId, msgId, btnIndex),
                "關閉",
                Emoji.fromUnicode("🔒")
            )
        }

        return ActionRow.of(
            lockButton,
            Button.of(
                ButtonStyle.PRIMARY,
                buildTicketActionId("save", userId, msgId, btnIndex),
                "儲存對話",
                Emoji.fromUnicode("💾")
            ),
            Button.of(
                ButtonStyle.DANGER,
                buildTicketActionId("delete", userId, msgId, btnIndex),
                "刪除",
                Emoji.fromUnicode("🗑")
            )
        )
    }

    private fun buildTicketActionId(action: String, userId: Long, msgId: String, btnIndex: String): String {
        return componentIdManager.build(
            mapOf(
                "action" to action,
                "user_id" to userId,
                "msg_id" to msgId,
                "btn_index" to btnIndex,
            )
        )
    }

    private fun hasTicketAdminPermission(member: Member, idMap: Map<String, Any>): Boolean {
        val roleIds = getTicketData(member.guild.idLong, idMap).adminIds
        return member.roles.any { roleIds.contains(it.idLong) } || member.hasPermission(ADMINISTRATOR)
    }

    private fun getTicketData(guildId: Long, idMap: Map<String, Any>) = jsonGuildManager
        .get(guildId)
        .data
        .get(idMap["msg_id"] as String)
        ?.get((idMap["btn_index"] as String).toInt())
        ?: throw IllegalStateException("Cannot find data.")

    private fun safeDeleteWebhook(webhook: Webhook) {
        webhook.delete().queue({}, {})
    }

    private fun unwrapThrowable(throwable: Throwable): Throwable {
        if (throwable is CompletionException && throwable.cause != null) {
            return throwable.cause!!
        }

        return throwable
    }

    private fun toReadableError(throwable: Throwable): String {
        val error = unwrapThrowable(throwable)
        return error.message
            ?.replace('\n', ' ')
            ?.take(200)
            ?: error.javaClass.simpleName
    }
}
