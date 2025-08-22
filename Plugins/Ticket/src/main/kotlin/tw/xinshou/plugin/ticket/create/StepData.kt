package tw.xinshou.plugin.ticket.create

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import tw.xinshou.core.util.ComponentField
import tw.xinshou.plugin.ticket.Ticket.componentIdManager
import tw.xinshou.plugin.ticket.Ticket.messageCreator
import tw.xinshou.plugin.ticket.json.serializer.DataContainer

internal class Step(
    val hook: InteractionHook,
    val message: Message? = null
) {
    val data = StepData()

    fun renderEmbedAction(locale: DiscordLocale): WebhookMessageEditAction<Message?> {
        val messageEditData = if (message == null) {
            messageCreator.getEditBuilder(
                "create-ticket",
                locale,
                modelMapper = mapOf(
                    "tt@embed-demo" to previewEmbed,
                    "tt@btn-demo" to previewComponent,
                )
            ).build()
        } else {
            messageCreator.getEditBuilder(
                "add-ticket",
                locale,
                modelMapper = mapOf(
                    "tt@embed-demo" to previewEmbed,
                    "tt@btn-demo" to previewComponent,
                )
            ).build()
        }

        return hook.editOriginal(messageEditData)
    }

    val json: DataContainer
        get() {
            return data.json
        }

    fun setAuthor(name: String?, iconURL: String?) {
        if (name == null) {
            data.author = null
            data.authorIconUrl = null
            return
        }

        data.author = name
        data.authorIconUrl = iconURL
    }

    fun setTitle(title: String?) {
        data.title = title
    }

    fun setDesc(desc: String?) {
        data.description = desc
    }

    fun setReason(reason: String) {
        if (reason.isEmpty()) return
        data.json.reasonTitle = reason
    }

    fun setAdminIds(adminId: List<Long>) {
        data.json.adminIds = adminId.toMutableList()
    }

    fun setCategoryId(categoryId: Long) {
        data.json.categoryId = categoryId
    }

    fun setColor(color: Int) {
        data.color = color
    }

    fun setBtnContent(content: String?) {
        data.btnText = content
    }

    fun setBtnEmoji(emoji: Emoji?) {
        data.btnEmoji = emoji
    }

    fun setBtnStyle(style: ButtonStyle?) {
        data.btnStyle = style
    }

    fun confirmCreateAction(locale: DiscordLocale, channel: MessageChannelUnion): RestAction<Message> {
        hook.deleteOriginal().queue()

        if (message == null) {
            // send a new message
            val createMessageData = messageCreator.getCreateBuilder(
                "confirm-create",
                locale,
                modelMapper = mapOf(
                    "tt@embed" to previewEmbed,
                    "tt@btn" to ButtonImpl(
                        componentIdManager.build(
                            ComponentField("btn_index", "0"),
                        ),
                        data.btnText,
                        data.btnStyle,
                        false,
                        data.btnEmoji
                    ),
                )
            ).build()

            return channel.sendMessage(createMessageData)
        }

        // modify the original message
        // TODO: currently, we only support modifying the message components
        //       we should also support modifying the message content soon
        val messageEditData = messageCreator.getEditBuilder(
            "confirm-add",
            locale,
            modelMapper = mapOf(
                "tt@embed" to previewEmbed,
                "tt@btn" to message.actionRows[0].components.apply {
                    add(
                        ButtonImpl(
                            componentIdManager.build(
                                ComponentField("btn_index", size.toString()),
                            ),
                            data.btnText,
                            data.btnStyle,
                            false,
                            data.btnEmoji
                        )
                    )
                }
            )
        ).build()

        return message.editMessage(messageEditData)
    }


    val previewEmbed: MessageEmbed
        get() {
            if (message == null) return EmbedBuilder()
                .setAuthor(data.author, null, data.authorIconUrl)
                .setTitle(data.title)
                .setDescription(data.description)
                .setColor(data.color).build()

            return message.embeds[0]
        }

    val previewComponent: ButtonImpl
        get() = ButtonImpl(
            componentIdManager.build(
                ComponentField("sub_action", "prev"),
            ), data.btnText, data.btnStyle, false, data.btnEmoji
        )

}


internal data class StepData(
    val json: DataContainer = DataContainer(
        reasonTitle = "有任何可以幫助的問題嗎~",
        adminIds = mutableListOf(),
        categoryId = 0
    ),
    var author: String? = "Ticket 服務",
    var authorIconUrl: String? =
        "https://img.lovepik.com/free-png/20211116/lovepik-customer-service-personnel-icon-png-image_400960955_wh1200.png",
    var title: String? = "\uD83D\uDEE0 聯絡我們",
    var description: String? = "✨ 點擊下方 **[按鈕]**，並提供所遭遇的問題，我們盡快給予答覆！ ✨",
    var color: Int = 0x00FFFF,
    var btnText: String? = "聯絡我們",
    var btnEmoji: Emoji? = Emoji.fromUnicode("✉"),
    var btnStyle: ButtonStyle? = ButtonStyle.SUCCESS,
)
