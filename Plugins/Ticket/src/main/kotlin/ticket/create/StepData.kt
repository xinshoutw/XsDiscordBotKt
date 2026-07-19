package tw.xinshou.discord.plugin.ticket.create

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import tw.xinshou.discord.plugin.ticket.Ticket.componentId
import tw.xinshou.discord.plugin.ticket.Ticket.messageTemplate
import tw.xinshou.discord.plugin.ticket.json.serializer.DataContainer

internal class Step(
    val hook: InteractionHook,
    val message: Message? = null
) {
    val data = StepData()

    fun renderEmbedAction(locale: DiscordLocale): WebhookMessageEditAction<Message?> {
        val messageEditData = if (message == null) {
            messageTemplate.buildEdit("create-ticket", locale).build()
        } else {
            messageTemplate.buildEdit("add-ticket", locale).build()
        }
        return hook.editOriginal(messageEditData)
    }

    val json: DataContainer get() = data.json

    fun setAuthor(name: String?, iconURL: String?) {
        if (name == null) { data.author = null; data.authorIconUrl = null; return }
        data.author = name; data.authorIconUrl = iconURL
    }
    fun setTitle(title: String?) { data.title = title }
    fun setDesc(desc: String?) { data.description = desc }
    fun setReason(reason: String) { if (reason.isEmpty()) return; data.json.reasonTitle = reason }
    fun setAdminIds(adminId: List<Long>) { data.json.adminIds = adminId.toMutableList() }
    fun setCategoryId(categoryId: Long) { data.json.categoryId = categoryId }
    fun setColor(color: Int) { data.color = color }
    fun setBtnContent(content: String?) { data.btnText = content }
    fun setBtnEmoji(emoji: Emoji?) { data.btnEmoji = emoji }
    fun setBtnStyle(style: ButtonStyle?) { data.btnStyle = style }

    fun confirmCreateAction(locale: DiscordLocale, channel: MessageChannelUnion): RestAction<Message> {
        hook.deleteOriginal().queue()
        if (message == null) {
            val createMessageData = messageTemplate.buildCreate("confirm-create", locale).build()
            return channel.sendMessage(createMessageData)
        }
        val actionRow = message.components.firstOrNull { it.type == Component.Type.ACTION_ROW }?.asActionRow()
            ?: throw IllegalStateException("Cannot find action row in the target message.")
        val updatedButtons = actionRow.components.map { it as ActionRowChildComponent }.toMutableList().apply { add(buildTicketButton(size.toString())) }
        val messageEditData = MessageEditBuilder().setEmbeds(previewEmbed).setComponents(ActionRow.of(updatedButtons)).build()
        return message.editMessage(messageEditData)
    }

    val previewEmbed: MessageEmbed
        get() = if (message == null) EmbedBuilder().setAuthor(data.author, null, data.authorIconUrl).setTitle(data.title).setDescription(data.description).setColor(data.color).build()
        else message.embeds[0]

    val previewComponent: Button
        get() = Button.of(data.btnStyle ?: ButtonStyle.SUCCESS, componentId.build("sub_action" to "prev"), data.btnText, data.btnEmoji)

    private fun buildTicketButton(index: String): Button {
        return Button.of(data.btnStyle ?: ButtonStyle.SUCCESS, componentId.build("action" to "press", "btn_index" to index), data.btnText, data.btnEmoji)
    }
}

internal data class StepData(
    val json: DataContainer = DataContainer(reasonTitle = "How can we help?", adminIds = mutableListOf(), categoryId = 0),
    var author: String? = "Ticket Service",
    var authorIconUrl: String? = "https://img.lovepik.com/free-png/20211116/lovepik-customer-service-personnel-icon-png-image_400960955_wh1200.png",
    var title: String? = "\uD83D\uDEE0 Contact Us",
    var description: String? = "Click the button below to create a ticket!",
    var color: Int = 0x00FFFF,
    var btnText: String? = "Contact Us",
    var btnEmoji: Emoji? = Emoji.fromUnicode("\u2709"),
    var btnStyle: ButtonStyle? = ButtonStyle.SUCCESS,
)
