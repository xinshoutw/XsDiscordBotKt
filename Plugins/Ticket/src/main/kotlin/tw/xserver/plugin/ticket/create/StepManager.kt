package tw.xserver.plugin.ticket.create

import com.google.gson.JsonArray
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import tw.xserver.loader.util.ComponentField
import tw.xserver.plugin.ticket.Ticket.componentIdManager
import tw.xserver.plugin.ticket.Ticket.jsonGuildManager

internal object StepManager {
    private val steps: HashMap<Long, Step> = hashMapOf()

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "create-ticket" -> onCreateTicket(event)
            "add-ticket" -> onAddTicket(event)
        }
    }

    private fun onCreateTicket(event: SlashCommandInteractionEvent) {
        val step = Step(event.hook)
        step.renderEmbedAction().queue()
        steps.put(event.user.idLong, step)
    }

    private fun onAddTicket(event: SlashCommandInteractionEvent) {
        val messageId = event.getOption("message_id")!!.asLong

        event.messageChannel.retrieveMessageById(messageId)
            .onErrorFlatMap { i -> event.hook.editOriginal("Cannot found the message by id: $messageId") }
            .flatMap {
                if (!it.actionRows.isEmpty() && it.actionRows[0].components.size == 5) {
                    event.hook.editOriginal("This message is full of buttons, please recreate a new message")
                } else {
                    val step = Step(event.hook, it)
                    steps.put(event.user.idLong, step)
                    step.renderEmbedAction()
                }
            }.queue()
    }


    /* ------------------------------------- */
    fun onButtonInteraction(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        when (idMap["sub_action"]) {
            "prev" -> previewReason(event)

            "author" -> authorForm(event)
            "content" -> content(event)
            "category" -> categoryMenu(event)
            "color" -> colorForm(event)

            "btnContent" -> btnContentForm(event)
            "btnColor" -> btnColorMenu(event)
            "reason" -> reasonForm(event)
            "admin" -> adminMenu(event)

            "confirm" -> confirmCreate(event)

            "back" -> backToMainMenu(event)

            // inside the color menu
            "btnColorSubmit" -> modifyBtnColor(event)
        }
    }


    private fun previewReason(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.replyModal(
                Modal.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "prev"),
                    ), step.data.reasonTitle
                ).addComponents(
                    ActionRow.of(TextInput.create("reason", "原因", TextInputStyle.PARAGRAPH).build())
                ).build()
            ).queue()
        }
    }

    private fun authorForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val nameInput = TextInput.create("author", "設定作者名稱", TextInputStyle.SHORT)
                .setValue(step.data.author)
                .setPlaceholder("Ticket 服務")
                .setMaxLength(256)
                .setRequired(false)
                .build()

            val imageInput = TextInput.create("image", "設定作者圖示", TextInputStyle.PARAGRAPH)
                .setValue(step.data.authorIconURL)
                .setPlaceholder("https://img .... 5_wh1200.png")
                .setMaxLength(4000)
                .setRequired(false)
                .build()

            event.replyModal(
                Modal.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "author"),
                    ), "創建客服單"
                ).addComponents(
                    ActionRow.of(nameInput), ActionRow.of(imageInput)
                ).build()
            ).queue()
        }
    }

    private fun content(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val titleInput = TextInput.create("title", "設定標題", TextInputStyle.SHORT)
                .setValue(step.data.title)
                .setPlaceholder("\uD83D\uDEE0 聯絡我們")
                .setMaxLength(256)
                .build()

            val descInput = TextInput.create("desc", "設定內文", TextInputStyle.PARAGRAPH)
                .setValue(step.data.description)
                .setPlaceholder("\uD83D\uDEE0 聯絡我們")
                .setMaxLength(4000)
                .build()

            event.replyModal(
                Modal.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "content"),
                    ), "創建客服單"
                ).addComponents(
                    ActionRow.of(titleInput), ActionRow.of(descInput)
                ).build()
            ).queue()
        }
    }

    private fun categoryMenu(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val menu =
                EntitySelectMenu.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "category"),
                    ), EntitySelectMenu.SelectTarget.CHANNEL
                )
                    .setChannelTypes(ChannelType.CATEGORY)
                    .setRequiredRange(1, 1)
                    .setPlaceholder("未設定則為預設")
                    .build()
            event.deferEdit().flatMap {
                step.hook.editOriginalComponents(
                    ActionRow.of(menu),
                    ActionRow.of(
                        Button.of(
                            ButtonStyle.PRIMARY,
                            componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "back"),
                            ), "返回"
                        )
                    )
                )
            }.queue()
        }
    }

    private fun colorForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val colorInput = TextInput.create("color", "設定顏色", TextInputStyle.SHORT)
                .setValue(String.format("#%06X", step.data.color and 0xFFFFFF))
                .setPlaceholder("0x00FFFF")
                .build()

            event.replyModal(
                Modal.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "color"),
                    ), "創建客服單"
                ).addComponents(
                    ActionRow.of(colorInput)
                ).build()
            ).queue()
        }
    }

    private fun btnContentForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val btnTextInput = TextInput.create("btnText", "設定按鈕文字", TextInputStyle.SHORT)
                .setValue(step.data.btnContent)
                .setPlaceholder("聯絡我們")
                .setMaxLength(80)
                .setRequired(false)
                .build()

            val btnEmojiInput = TextInput.create("btnEmoji", "設定按鈕符號", TextInputStyle.SHORT)
                .setValue(step.data.btnEmoji?.asReactionCode ?: "✉")
                .setPlaceholder("✉")
                .setRequired(false)
                .build()

            event.replyModal(
                Modal.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "btnContent"),
                    ), "創建客服單"
                ).addComponents(
                    ActionRow.of(btnTextInput),
                    ActionRow.of(btnEmojiInput)
                ).build()
            ).queue()
        }
    }

    private fun btnColorMenu(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.deferEdit().flatMap {
                step.hook.editOriginalComponents(
                    ActionRow.of(
                        Button.of(
                            ButtonStyle.SUCCESS, componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "btnColorSubmit"),
                                ComponentField("color_index", 3),
                            ), "綠色"
                        ),
                        Button.of(
                            ButtonStyle.PRIMARY, componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "btnColorSubmit"),
                                ComponentField("color_index", 1),
                            ), "藍色"
                        ),
                        Button.of(
                            ButtonStyle.DANGER, componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "btnColorSubmit"),
                                ComponentField("color_index", 4),
                            ), "紅色"
                        ),
                        Button.of(
                            ButtonStyle.SECONDARY, componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "btnColorSubmit"),
                                ComponentField("color_index", 2),
                            ), "灰色"
                        ),
                    ),
                    ActionRow.of(
                        Button.of(
                            ButtonStyle.PRIMARY, componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "back"),
                            ), "返回"
                        )
                    )
                )
            }.queue()
        }
    }

    private fun reasonForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val reasonInput = TextInput.create("reason", "設定原因", TextInputStyle.PARAGRAPH)
                .setValue(step.data.reasonTitle)
                .setPlaceholder("有任何可以幫助的問題嗎~")
                .setMaxLength(45)
                .build()

            event.replyModal(
                Modal.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "reason"),
                    ), "創建客服單"
                ).addComponents(
                    ActionRow.of(reasonInput)
                ).build()
            ).queue()
        }
    }

    private fun adminMenu(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val menu =
                EntitySelectMenu.create(
                    componentIdManager.build(
                        ComponentField("action", "create"),
                        ComponentField("sub_action", "admin"),
                    ), EntitySelectMenu.SelectTarget.ROLE
                ).setMaxValues(25).build()

            event.deferEdit().flatMap {
                step.hook.editOriginalComponents(
                    ActionRow.of(menu),
                    ActionRow.of(
                        Button.of(
                            ButtonStyle.PRIMARY, componentIdManager.build(
                                ComponentField("action", "create"),
                                ComponentField("sub_action", "back"),
                            ), "返回"
                        )
                    )
                )
            }.queue()
        }
    }

    private fun confirmCreate(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val manager = jsonGuildManager[event.guild!!.idLong]
            step.confirmCreateAction(event.channel).map {
                manager.computeIfAbsent(it.id, JsonArray()).asJsonArray.add(step.json)
                manager.save()
            }
        }
    }

    private fun backToMainMenu(event: ButtonInteractionEvent) {
        event.deferEdit().flatMap {
            steps[event.user.idLong]?.renderEmbedAction()
        }.queue()
    }

    private fun modifyBtnColor(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val idMap = componentIdManager.parse(event.componentId)
            if (idMap["sub_action"] == "btnColorSubmit") {
                step.setBtnStyle(ButtonStyle.fromKey(idMap["color_index"] as Int))
                event.deferEdit().flatMap {
                    step.renderEmbedAction()
                }.queue()
            }
        }
    }


    /* ------------------------------------- */
    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val idMap = componentIdManager.parse(event.componentId)
            when (idMap["sub_action"]) {
                "admin" -> {
                    step.setAdminIds(event.values.map { it.idLong })
                }

                "category" -> {
                    val categoryId = event.values[0].idLong
                    if (categoryId != 0L && event.guild!!.getCategoryById(categoryId) == null) {
                        event.reply("Cannot find a category with the id ${categoryId}!").setEphemeral(true).queue()
                        return
                    }

                    step.setCategoryId(categoryId)
                }
            }

            event.deferEdit().flatMap {
                step.renderEmbedAction()
            }.queue()
        }
    }


    /* ------------------------------------- */
    fun onModalInteraction(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        steps[event.user.idLong]?.let { step ->
            when (idMap["sub_action"]) {
                "author" -> {
                    step.setAuthor(event.getValue("author")?.asString, event.getValue("image")?.asString)
                }

                "content" -> {
                    step.setTitle(event.getValue("title")?.asString)
                    step.setDesc(event.getValue("desc")?.asString)
                }

                "reason" -> {
                    step.setReason(event.getValue("reason")!!.asString)
                }

                "color" -> {
                    step.setColor(
                        Integer.parseInt(event.getValue("color")!!.asString.substring(1), 16)
                    )
                }

                "btnContent" -> {
                    val btnText = event.getValue("btnText")
                    val btnEmoji = event.getValue("btnEmoji")

                    if (btnText == null && btnEmoji == null) {
                        event.reply("Either ButtonText or ButtonEmoji must be provided!").setEphemeral(true).queue()
                        return
                    }

                    step.setBtnContent(btnText?.asString)
                    step.setBtnEmoji(
                        btnEmoji?.let {
                            if (it.asString.isNotEmpty()) Emoji.fromUnicode(it.asString)
                            else null
                        }
                    )
                }
            }

            event.deferEdit().flatMap {
                step.renderEmbedAction()
            }.queue()
        }
    }


    /* ------------------------------------- */
    fun onGuildLeave(event: GuildLeaveEvent) {
        jsonGuildManager[event.guild.idLong].delete()
    }
}
