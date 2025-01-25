package tw.xserver.plugin.ticket.create

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import tw.xserver.loader.util.ComponentField
import tw.xserver.plugin.ticket.Ticket.componentIdManager

internal class Step(
    val hook: InteractionHook,
    val message: Message? = null
) {
    val data = StepData()

    fun renderEmbedAction(): WebhookMessageEditAction<Message?> {
        val actions = ArrayList<LayoutComponent>().apply {
            add(ActionRow.of(getPreviewComponent))

            if (message == null) {
                add(
                    ActionRow.of( // TODO: change suffix to btn
                        ButtonImpl(
                            componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "author_btn"),
                            ), "設定作者", ButtonStyle.PRIMARY, false, null
                        ),
                        ButtonImpl(
                            componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "content_btn"),
                            ), "設定文字", ButtonStyle.PRIMARY, false, null
                        ),
                        ButtonImpl(
                            componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "category_btn"),
                            ), "設定頻道目錄", ButtonStyle.PRIMARY, false, null
                        ),
                        ButtonImpl(
                            componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "color_btn"),
                            ), "設定顏色", ButtonStyle.PRIMARY, false, null
                        )
                    )
                )
            }

            add(
                ActionRow.of( // TODO: change suffix to btn
                    ButtonImpl(
                        componentIdManager.build(
                            ComponentField("action", "create"),
                            ComponentField("sub_action", "btn_content_btn"),
                        ), "設定按鈕文字", ButtonStyle.PRIMARY, false, null
                    ),
                    ButtonImpl(
                        componentIdManager.build(
                            ComponentField("action", "create"),
                            ComponentField("sub_action", "btn_color_btn"),
                        ), "設定按鈕顏色", ButtonStyle.PRIMARY, false, null
                    ),
                    ButtonImpl(
                        componentIdManager.build(
                            ComponentField("action", "create"),
                            ComponentField("sub_action", "reason_btn"),
                        ), "設定詢問標題", ButtonStyle.PRIMARY, false, null
                    ),
                    ButtonImpl(
                        componentIdManager.build(
                            ComponentField("action", "create"),
                            ComponentField("sub_action", "admin_btn"),
                        ), "設定允許身分組", ButtonStyle.PRIMARY, false, null
                    ),
                    ButtonImpl(
                        componentIdManager.build(
                            ComponentField("action", "create"),
                            ComponentField("sub_action", "confirm_btn"),
                        ), "確定建立", ButtonStyle.SUCCESS, false, null
                    )
                )
            )
        }

        return hook.editOriginalEmbeds(previewEmbed).setComponents(actions)
    }

    val json: JsonObject
        get() {
            val obj = JsonObject()
            val adminIds = JsonArray().apply {
                data.adminIds?.let {
                    for (i in it) add(i)
                }
            }

            obj.addProperty("reasonTitle", data.reasonTitle)
            obj.add("adminIds", adminIds)
            obj.addProperty("categoryId", data.categoryId)

            return obj
        }

    fun setAuthor(name: String?, iconURL: String?) {
        if (name == null) {
            data.author = null
            data.authorIconURL = null
            return
        }

        data.author = name
        data.authorIconURL = iconURL
    }

    fun setTitle(title: String?) {
        data.title = title
    }

    fun setDesc(desc: String?) {
        data.description = desc
    }

    fun setReason(reason: String) {
        if (reason.isEmpty()) return
        data.reasonTitle = reason
    }

    fun setAdminIds(adminId: List<Long>?) {
        data.adminIds = adminId
    }

    fun setCategoryId(categoryId: Long) {
        data.categoryId = categoryId
    }

    fun setColor(color: Int) {
        data.color = color
    }

    fun setBtnContent(content: String?) {
        data.btnContent = content
    }

    fun setBtnEmoji(emoji: Emoji?) {
        data.btnEmoji = emoji
    }

    fun setBtnStyle(style: ButtonStyle?) {
        data.btnStyle = style
    }

    fun confirmCreateAction(channel: MessageChannelUnion): RestAction<Message> {
        hook.deleteOriginal().queue()

        if (message == null) {
            // send a new message
            return channel.sendMessageEmbeds(this.previewEmbed).flatMap {
                it.editMessageComponents(
                    ActionRow.of(
                        ButtonImpl(
                            componentIdManager.build(
                                ComponentField("action", "press"),
                                ComponentField("btn_index", 0),
                            ),
                            data.btnContent,
                            data.btnStyle,
                            false,
                            data.btnEmoji
                        )
                    )
                )
            }
        }

        // modify the original message
        val rowData = message.actionRows[0].components.apply {
            add(
                ButtonImpl(
                    componentIdManager.build(
                        ComponentField("action", "press"),
                        ComponentField("btn_index", size),
                    ),
                    data.btnContent,
                    data.btnStyle,
                    false,
                    data.btnEmoji
                )
            )
        }
        return message.editMessageComponents(ActionRow.of(rowData))
    }


    private val previewEmbed: MessageEmbed
        get() {
            if (message == null) return EmbedBuilder()
                .setAuthor(data.author, null, data.authorIconURL)
                .setTitle(data.title)
                .setDescription(data.description)
                .setColor(data.color).build()

            return message.embeds[0]
        }

    private val getPreviewComponent: ButtonImpl
        get() = ButtonImpl(
            componentIdManager.build(
                ComponentField("action", "create"),
                ComponentField("sub_action", "prev"),
            ), data.btnContent, data.btnStyle, false, data.btnEmoji
        )

}

internal data class StepData(
    var author: String? = "Ticket 服務",
    var authorIconURL: String? =
        "https://img.lovepik.com/free-png/20211116/lovepik-customer-service-personnel-icon-png-image_400960955_wh1200.png",
    var title: String? = "\uD83D\uDEE0 聯絡我們",
    var description: String? = "✨ 點擊下方 **[按鈕]**，並提供所遭遇的問題，我們盡快給予答覆！ ✨",
    var color: Int = 0x00FFFF,
    var reasonTitle: String = "有任何可以幫助的問題嗎~",
    var adminIds: List<Long>? = listOf(),
    var btnContent: String? = "聯絡我們",
    var btnEmoji: Emoji? = Emoji.fromUnicode("✉"),
    var btnStyle: ButtonStyle? = ButtonStyle.SUCCESS,
    var categoryId: Long = 0,
)
