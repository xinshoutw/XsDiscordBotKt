package tw.xserver.plugin.ticket.create

import com.google.gson.JsonArray
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.plugin.ticket.Ticket.componentIdManager
import tw.xserver.plugin.ticket.Ticket.jsonGuildManager
import tw.xserver.plugin.ticket.Ticket.messageCreator
import tw.xserver.plugin.ticket.Ticket.modalCreator

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
        step.renderEmbedAction(event.userLocale).queue()
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
                    step.renderEmbedAction(event.userLocale)
                }
            }.queue()
    }


    /* ------------------------------------- */
    fun onButtonInteraction(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        when (idMap["sub_action"]) { // TODO: replace to new key
            "prev" -> previewReason(event)

            "author" -> authorForm(event)
            "content" -> content(event)
            "category" -> categoryMenu(event)
            "color" -> colorForm(event)

            "btnContent" -> btnTextForm(event)
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
                modalCreator.getModalBuilder(
                    "preview-reason",
                    event.userLocale,
                    substitutor = Placeholder.getSubstitutor(event).put(
                        "tt@reason-title", step.data.reasonTitle
                    )
                ).build()
            ).queue()
        }
    }

    private fun authorForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.replyModal(
                modalCreator.getModalBuilder(
                    "author-form",
                    event.userLocale,
                    substitutor = Placeholder.getSubstitutor(event).putAll(
                        "tt@author" to (step.data.author ?: ""),
                        "tt@image" to (step.data.authorIconUrl ?: "")
                    )
                ).build()
            ).queue()
        }
    }

    private fun content(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.replyModal(
                modalCreator.getModalBuilder(
                    "content-form",
                    event.userLocale,
                    substitutor = Placeholder.getSubstitutor(event).putAll(
                        "tt@title" to (step.data.title ?: ""),
                        "tt@description" to (step.data.description ?: "")
                    )
                ).build()
            ).queue()
        }
    }

    private fun categoryMenu(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.deferEdit().flatMap {
                step.hook.editOriginal(
                    messageCreator.getEditBuilder(
                        "modify-category",
                        event.userLocale,
                        modelMapper = mapOf(
                            "tt@embed-demo" to step.previewEmbed,
                            "tt@btn-demo" to step.previewComponent,
                        )
                    ).build()
                )
            }.queue()
        }
    }

    private fun colorForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.replyModal(
                modalCreator.getModalBuilder(
                    "modify-embed-color",
                    event.userLocale,
                    substitutor = Placeholder.getSubstitutor(event).put(
                        "tt@hex-color", String.format("#%06X", step.data.color and 0xFFFFFF)
                    )
                ).build()
            ).queue()
        }
    }

    private fun btnTextForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.replyModal(
                modalCreator.getModalBuilder(
                    "modify-btn-text",
                    event.userLocale,
                    substitutor = Placeholder.getSubstitutor(event).putAll(
                        "tt@btn-text" to (step.data.btnText ?: ""),
                        "tt@btn-emoji" to (step.data.btnEmoji?.asReactionCode ?: "")
                    )
                ).build()
            ).queue()
        }
    }

    private fun btnColorMenu(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.deferEdit().flatMap {
                it.editOriginal(
                    messageCreator.getEditBuilder(
                        "modify-btn-color",
                        event.userLocale,
                        modelMapper = mapOf(
                            "tt@embed-demo" to step.previewEmbed,
                            "tt@btn-demo" to step.previewComponent,
                        )
                    ).build()
                )
            }.queue()
        }
    }

    private fun reasonForm(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.replyModal(
                modalCreator.getModalBuilder(
                    "reason-form",
                    event.userLocale,
                    modelMapper = mapOf(
                        "tt@reason-title" to step.data.reasonTitle
                    )
                ).build()
            ).queue()
        }
    }

    private fun adminMenu(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            event.deferEdit().flatMap {
                step.hook.editOriginal(
                    messageCreator.getEditBuilder(
                        "modify-admin-role",
                        event.userLocale,
                        modelMapper = mapOf(
                            "tt@embed-demo" to step.previewEmbed,
                            "tt@btn-demo" to step.previewComponent,
                        )
                    ).build()
                )
            }.queue()
        }
    }

    private fun confirmCreate(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val manager = jsonGuildManager[event.guild!!.idLong]
            step.confirmCreateAction(event.userLocale, event.channel).map {
                manager.computeIfAbsent(it.id, JsonArray()).asJsonArray.add(step.json)
                manager.save()
            }
        }
    }

    private fun backToMainMenu(event: ButtonInteractionEvent) {
        event.deferEdit().flatMap {
            steps[event.user.idLong]?.renderEmbedAction(event.userLocale)
        }.queue()
    }

    private fun modifyBtnColor(event: ButtonInteractionEvent) {
        steps[event.user.idLong]?.let { step ->
            val idMap = componentIdManager.parse(event.componentId)
            if (idMap["sub_action"] == "btnColorSubmit") {
                step.setBtnStyle(ButtonStyle.fromKey(idMap["color_index"] as Int))
                event.deferEdit().flatMap {
                    step.renderEmbedAction(event.userLocale)
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
                step.renderEmbedAction(event.userLocale)
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
                step.renderEmbedAction(event.userLocale)
            }.queue()
        }
    }


    /* ------------------------------------- */
    fun onGuildLeave(event: GuildLeaveEvent) {
        jsonGuildManager[event.guild.idLong].delete()
    }
}
