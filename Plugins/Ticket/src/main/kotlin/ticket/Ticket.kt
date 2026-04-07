package tw.xinshou.discord.plugin.ticket

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import core.placeholder.withUser
import core.placeholder.withMember
import core.util.ComponentId
import core.util.GuildJsonFile
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction.PaginationOrder.FORWARD
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import tw.xinshou.discord.plugin.ticket.Event.config
import tw.xinshou.discord.plugin.ticket.Event.pluginDirectory
import tw.xinshou.discord.plugin.ticket.create.StepManager
import tw.xinshou.discord.plugin.ticket.json.serializer.DataContainer
import tw.xinshou.discord.plugin.ticket.json.serializer.JsonDataClass
import java.io.File
import java.text.BreakIterator
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CompletionException

internal object Ticket {
    private const val archivePageSize = 100
    private const val threadNameMaxLength = 100
    private val threadTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")
    private val archiveMarkerTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    val jsonGuildManager = GuildJsonFile(
        directory = File(pluginDirectory, "data"),
        serializer = MapSerializer(String.serializer(), ListSerializer(DataContainer.serializer())),
        defaultInstance = { mutableMapOf() },
    )

    val componentId = ComponentId(
        prefix = config.componentPrefix,
        idKeys = mapOf(
            "action" to ComponentId.FieldType.STRING,
            "sub_action" to ComponentId.FieldType.STRING,
            "color_index" to ComponentId.FieldType.STRING,
            "user_id" to ComponentId.FieldType.LONG_HEX,
            "msg_id" to ComponentId.FieldType.STRING,
            "btn_index" to ComponentId.FieldType.STRING,
        )
    )

    var messageTemplate = MessageTemplate(
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
        StepManager.onSlashCommandInteraction(event)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val idMap = componentId.parse(event.componentId)
        val guild = event.guild!!

        when (idMap["action"]) {
            "create" -> StepManager.onButtonInteraction(event, idMap)
            "press" -> {
                val ticketData = getTicketDataOrNull(guild.idLong, idMap, event.messageId)
                if (ticketData == null) {
                    event.reply(ticketDataNotFoundMessage).setEphemeral(true).queue(); return
                }
                val reason = ticketData.reasonTitle
                val modalId = componentId.build(
                    "action" to "submit",
                    "msg_id" to event.messageId,
                    "btn_index" to (idMap["btn_index"] as String),
                )
                val modal = Modal.create(modalId, "Ticket")
                    .addComponents(Label.of(reason, TextInput.create("reason", TextInputStyle.PARAGRAPH).setRequired(true).build()))
                    .build()
                event.replyModal(modal).queue()
            }
            "lock" -> {
                val channel = event.guildChannel.asTextChannel()
                guild.retrieveMemberById(idMap["user_id"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).deny(VIEW_CHANNEL) }
                    .flatMap {
                        event.editComponents(buildTicketActionRow(true, idMap["user_id"] as Long, idMap["msg_id"] as String, idMap["btn_index"] as String))
                    }.queue()
            }
            "unlock" -> {
                val channel = event.guildChannel.asTextChannel()
                guild.retrieveMemberById(idMap["user_id"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).grant(VIEW_CHANNEL) }
                    .flatMap {
                        event.editComponents(buildTicketActionRow(false, idMap["user_id"] as Long, idMap["msg_id"] as String, idMap["btn_index"] as String))
                    }.queue()
            }
            "delete" -> {
                val member = event.member ?: return event.deferEdit().queue()
                val ticketData = getTicketDataOrNull(guild.idLong, idMap)
                if (ticketData == null) { event.reply(ticketDataNotFoundMessage).setEphemeral(true).queue(); return }
                if (hasTicketAdminPermission(member, ticketData)) {
                    event.deferEdit().flatMap { event.guildChannel.asTextChannel().delete() }.queue()
                } else { event.deferEdit().queue() }
            }
            "save" -> {
                val member = event.member ?: return event.deferEdit().queue()
                val ticketData = getTicketDataOrNull(guild.idLong, idMap)
                if (ticketData == null) { event.reply(ticketDataNotFoundMessage).setEphemeral(true).queue(); return }
                if (!hasTicketAdminPermission(member, ticketData)) {
                    event.reply("Only admin can use this.").setEphemeral(true).queue(); return
                }
                val selectMenu = EntitySelectMenu.create(
                    componentId.build("action" to "save", "sub_action" to "select-channel", "user_id" to idMap["user_id"] as Long, "msg_id" to idMap["msg_id"] as String, "btn_index" to idMap["btn_index"] as String),
                    EntitySelectMenu.SelectTarget.CHANNEL
                ).setRequiredRange(1, 1).setChannelTypes(ChannelType.TEXT).build()
                event.reply("Please select a text channel to save.").setEphemeral(true).addComponents(ActionRow.of(selectMenu)).queue()
            }
        }
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val idMap = componentId.parse(event.componentId)
        when (idMap["action"]) {
            "create" -> StepManager.onEntitySelectInteraction(event)
            "save" -> onSaveContentSelect(event, idMap)
        }
    }

    fun onGuildLeave(event: GuildLeaveEvent) { StepManager.onGuildLeave(event) }

    fun onModalInteraction(event: ModalInteractionEvent) {
        val idMap = componentId.parse(event.modalId)
        if (idMap["action"] == "create") { StepManager.onModalInteraction(event, idMap); return }
        if (idMap["action"] != "submit") return
        event.deferReply(true).queue()

        val guild = event.guild!!
        val jsonData = jsonGuildManager[guild.idLong].data[(idMap["msg_id"] as String)]!![(idMap["btn_index"] as String).toInt()]
        val reason = event.getValue("reason")!!.asString
        val roleIds = jsonData.adminIds
        val categoryId = jsonData.categoryId
        val category: Category? = if (categoryId == 0L) event.guildChannel.asTextChannel().parentCategory else guild.getCategoryById(categoryId)

        if (category == null) { event.hook.editOriginal("Error (category not found)").queue(); return }

        val builder = StringBuilder()
        category.createTextChannel(event.user.name).apply {
            addPermissionOverride(guild.publicRole, Permission.getRaw(), VIEW_CHANNEL.rawValue)
            addMemberPermissionOverride(event.user.idLong, VIEW_CHANNEL.rawValue, Permission.getRaw())
            for (roleId in roleIds) {
                addRolePermissionOverride(roleId, VIEW_CHANNEL.rawValue, Permission.getRaw())
                builder.append("<@&").append(roleId).append("> ")
            }
            builder.append("\n\n").append(reason)
        }.flatMap {
            event.hook.sendMessage("Please go to <#${it.id}> and wait for response!").queue()
            it.sendMessage(builder.toString()).addComponents(
                buildTicketActionRow(false, event.user.idLong, idMap["msg_id"] as String, idMap["btn_index"] as String)
            )
        }.queue()
    }

    private fun onSaveContentSelect(event: EntitySelectInteractionEvent, idMap: Map<String, Any>) {
        if (idMap["sub_action"] != "select-channel") { event.deferEdit().queue(); return }
        val member = event.member ?: run { event.reply("Error").setEphemeral(true).queue(); return }
        val ticketData = getTicketDataOrNull(member.guild.idLong, idMap)
        if (ticketData == null) { event.reply(ticketDataNotFoundMessage).setEphemeral(true).queue(); return }
        if (!hasTicketAdminPermission(member, ticketData)) { event.reply("Only admin can use this.").setEphemeral(true).queue(); return }
        val sourceChannel = event.guildChannel as? TextChannel ?: run { event.reply("Error").setEphemeral(true).queue(); return }
        val targetChannel = event.mentions.getChannels(TextChannel::class.java).firstOrNull() ?: run { event.reply("Please select a text channel.").setEphemeral(true).queue(); return }

        event.deferReply(true).queue { hook ->
            hook.editOriginal("Saving...").queue()
            archiveTicketContent(sourceChannel, targetChannel,
                { thread -> hook.editOriginal("Saved to <#${thread.id}>").queue() },
                { throwable -> hook.editOriginal("Save failed: ${toReadableError(throwable)}").queue() })
        }
    }

    private fun archiveTicketContent(sourceChannel: TextChannel, targetChannel: TextChannel, onSuccess: (ThreadChannel) -> Unit, onError: (Throwable) -> Unit) {
        targetChannel.createThreadChannel(buildArchiveThreadName(sourceChannel.name)).queue({ thread ->
            targetChannel.createWebhook(buildArchiveWebhookName()).queue({ webhook ->
                val paginator = sourceChannel.iterableHistory.order(FORWARD).cache(false)
                archiveTicketContentByPages(paginator, webhook, thread, mutableSetOf(), { safeDeleteWebhook(webhook); onSuccess(thread) }, { safeDeleteWebhook(webhook); onError(it) })
            }, onError)
        }, onError)
    }

    private fun archiveTicketContentByPages(paginator: MessagePaginationAction, webhook: Webhook, thread: ThreadChannel, processedThreadIds: MutableSet<Long>, onComplete: () -> Unit, onError: (Throwable) -> Unit) {
        paginator.takeAsync(archivePageSize).whenComplete { messages, throwable ->
            if (throwable != null) { onError(unwrapThrowable(throwable)); return@whenComplete }
            val pageMessages = messages ?: emptyList()
            if (pageMessages.isEmpty()) { onComplete(); return@whenComplete }
            forwardMessagesWithWebhook(webhook, thread, pageMessages, processedThreadIds, true, 0, { archiveTicketContentByPages(paginator, webhook, thread, processedThreadIds, onComplete, onError) }, onError)
        }
    }

    private fun forwardMessagesWithWebhook(webhook: Webhook, thread: ThreadChannel, messages: List<Message>, processedThreadIds: MutableSet<Long>, flattenThreadContent: Boolean, index: Int, onComplete: () -> Unit, onError: (Throwable) -> Unit) {
        var nextIndex = index
        while (nextIndex < messages.size && !shouldForwardMessage(messages[nextIndex])) nextIndex++
        if (nextIndex >= messages.size) { onComplete(); return }
        val message = messages[nextIndex]
        if (!shouldForwardMessage(message)) { onComplete(); return }
        webhook.sendMessage(buildForwardMessageData(message)).setThread(thread).setUsername(message.member?.effectiveName ?: message.author.name).setAvatarUrl(message.member?.effectiveAvatarUrl ?: message.author.effectiveAvatarUrl).queue({
            forwardMessagesWithWebhook(webhook, thread, messages, processedThreadIds, flattenThreadContent, nextIndex + 1, onComplete, onError)
        }, onError)
    }

    private fun buildForwardMessageData(message: Message) = MessageCreateBuilder().apply {
        if (message.contentRaw.isNotEmpty()) setContent(message.contentRaw)
        if (message.attachments.isNotEmpty()) setFiles(message.attachments.map { it.proxy.downloadAsFileUpload(it.fileName) })
    }.build()

    private fun shouldForwardMessage(message: Message) = message.contentRaw.isNotEmpty() || message.attachments.isNotEmpty()
    private fun buildArchiveWebhookName() = "ticket-save-${System.currentTimeMillis().toString(36)}"
    private fun buildArchiveThreadName(ticketName: String): String {
        val timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(threadTimeFormatter)
        val suffix = "-$timestamp"
        val maxLen = threadNameMaxLength - suffix.length
        return trimThreadNameByGrapheme(ticketName, maxLen) + suffix
    }
    private fun trimThreadNameByGrapheme(value: String, maxLength: Int): String {
        if (maxLength <= 0) return ""; if (value.length <= maxLength) return value
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT); iterator.setText(value)
        var boundary = iterator.first(); var lastSafeBoundary = 0
        while (boundary != BreakIterator.DONE) { if (boundary > maxLength) break; lastSafeBoundary = boundary; boundary = iterator.next() }
        return value.substring(0, lastSafeBoundary)
    }

    private fun buildTicketActionRow(isLocked: Boolean, userId: Long, msgId: String, btnIndex: String): ActionRow {
        val lockButton = if (isLocked) Button.of(ButtonStyle.SUCCESS, buildTicketActionId("unlock", userId, msgId, btnIndex), "Open", Emoji.fromUnicode("\uD83D\uDD13"))
        else Button.of(ButtonStyle.SECONDARY, buildTicketActionId("lock", userId, msgId, btnIndex), "Close", Emoji.fromUnicode("\uD83D\uDD12"))
        return ActionRow.of(lockButton, Button.of(ButtonStyle.PRIMARY, buildTicketActionId("save", userId, msgId, btnIndex), "Save", Emoji.fromUnicode("\uD83D\uDCBE")), Button.of(ButtonStyle.DANGER, buildTicketActionId("delete", userId, msgId, btnIndex), "Delete", Emoji.fromUnicode("\uD83D\uDDD1")))
    }

    private fun buildTicketActionId(action: String, userId: Long, msgId: String, btnIndex: String) = componentId.build("action" to action, "user_id" to userId, "msg_id" to msgId, "btn_index" to btnIndex)
    private fun hasTicketAdminPermission(member: Member, ticketData: DataContainer) = member.roles.any { ticketData.adminIds.contains(it.idLong) } || member.hasPermission(ADMINISTRATOR)

    private fun getTicketDataOrNull(guildId: Long, idMap: Map<String, Any>, fallbackMessageId: String? = null): DataContainer? {
        val messageId = (idMap["msg_id"] as? String) ?: fallbackMessageId ?: return null
        val buttonIndex = (idMap["btn_index"] as? String)?.toIntOrNull() ?: return null
        return jsonGuildManager[guildId].data[messageId]?.getOrNull(buttonIndex)
    }

    private val ticketDataNotFoundMessage = "Error (ticket data not found)"
    private fun safeDeleteWebhook(webhook: Webhook) { webhook.delete().queue({}, {}) }
    private fun unwrapThrowable(throwable: Throwable): Throwable { var c = throwable; while (c is CompletionException && c.cause != null && c.cause !== c) c = c.cause!!; return c }
    private fun toReadableError(throwable: Throwable) = unwrapThrowable(throwable).message?.replace('\n', ' ')?.take(200) ?: throwable.javaClass.simpleName
}
