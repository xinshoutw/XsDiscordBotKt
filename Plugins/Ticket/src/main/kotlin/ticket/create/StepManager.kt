package tw.xinshou.discord.plugin.ticket.create

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.plugin.ticket.Ticket.componentIdManager
import tw.xinshou.discord.plugin.ticket.Ticket.jsonGuildManager
import tw.xinshou.discord.plugin.ticket.Ticket.messageCreator
import tw.xinshou.discord.plugin.ticket.Ticket.modalCreator

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
                val firstActionRow = it.components
                    .firstOrNull { component -> component.type == Component.Type.ACTION_ROW }
                    ?.asActionRow()

                if (firstActionRow != null && firstActionRow.components.size == 5) {
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
        when (idMap["sub_action"]) {
            "prev" -> previewReason(event)

            "modify-author" -> authorForm(event)
            "modify-content" -> content(event)
            "modify-category" -> categoryMenu(event)
            "modify-color" -> colorForm(event)

            "modify-btn-text" -> btnTextForm(event)
            "modify-btn-color" -> btnColorMenu(event)
            "modify-reason" -> reasonForm(event)
            "modify-admin" -> adminMenu(event)

            "confirm-create" -> confirmCreate(event)

            "back" -> backToMainMenu(event)

            // inside the color menu
            "modify-btn-color-submit" -> modifyBtnColor(event, idMap)
        }
    }


    private fun previewReason(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return

        event.replyModal(
            modalCreator.getModalBuilder(
                "preview-reason",
                event.userLocale,
                substitutor = Placeholder.get(event).put(
                    "tt@reason-title", step.data.json.reasonTitle
                )
            ).build()
        ).queue()
    }

    private fun authorForm(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        event.replyModal(
            modalCreator.getModalBuilder(
                "modify-author",
                event.userLocale,
                substitutor = Placeholder.get(event).putAll(
                    "tt@author" to (step.data.author ?: ""),
                    "tt@author-icon-url" to (step.data.authorIconUrl ?: "")
                )
            ).build()
        ).queue()
    }

    private fun content(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        event.replyModal(
            modalCreator.getModalBuilder(
                "modify-content",
                event.userLocale,
                substitutor = Placeholder.get(event).putAll(
                    "tt@title" to (step.data.title ?: ""),
                    "tt@description" to (step.data.description ?: "")
                )
            ).build()
        ).queue()
    }

    private fun categoryMenu(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

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

    private fun colorForm(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        event.replyModal(
            modalCreator.getModalBuilder(
                "modify-embed-color",
                event.userLocale,
                substitutor = Placeholder.get(event).put(
                    "tt@hex-color", String.format("#%06X", step.data.color and 0xFFFFFF)
                )
            ).build()
        ).queue()
    }

    private fun btnTextForm(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        event.replyModal(
            modalCreator.getModalBuilder(
                "modify-btn-text",
                event.userLocale,
                substitutor = Placeholder.get(event).putAll(
                    "tt@btn-text" to (step.data.btnText ?: ""),
                    "tt@btn-emoji" to (step.data.btnEmoji?.asReactionCode ?: "")
                )
            ).build()
        ).queue()
    }

    private fun btnColorMenu(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

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

    private fun reasonForm(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        event.replyModal(
            modalCreator.getModalBuilder(
                "modify-reason-title",
                event.userLocale,
                substitutor = Placeholder.get(event).putAll(
                    mapOf(
                        "tt@reason-title" to step.data.json.reasonTitle
                    )
                )
            ).build()
        ).queue()
    }

    private fun adminMenu(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

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

    private fun confirmCreate(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        val manager = jsonGuildManager[event.guild!!.idLong]
        step.confirmCreateAction(event.userLocale, event.channel).map {
            val tmp = manager.data.get(it.id)
            if (tmp == null) {
                manager.data.put(it.id, mutableListOf(step.json))
            } else {
                tmp.add(step.json)
            }
            manager.save()
        }.queue()
    }

    private fun backToMainMenu(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        event.deferEdit().flatMap {
            step.renderEmbedAction(event.userLocale)
        }.queue()
    }

    private fun modifyBtnColor(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        step.setBtnStyle(ButtonStyle.fromKey((idMap["color_index"] as String).toInt()))
        event.deferEdit().flatMap {
            step.renderEmbedAction(event.userLocale)
        }.queue()
    }


    /* ------------------------------------- */
    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["sub_action"]) {
            "modify-admin" -> {
                step.setAdminIds(event.values.map { it.idLong })
            }

            "modify-category" -> {
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


    /* ------------------------------------- */
    fun onModalInteraction(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()

        when (idMap["sub_action"]) {
            "modify-author" -> {
                step.setAuthor(event.getValue("author")?.asString, event.getValue("image")?.asString)
            }

            "modify-content" -> {
                step.setTitle(event.getValue("title")?.asString)
                step.setDesc(event.getValue("desc")?.asString)
            }

            "modify-reason" -> {
                step.setReason(event.getValue("reason")!!.asString)
            }

            "modify-embed-color" -> {
                step.setColor(
                    Integer.parseInt(event.getValue("color")!!.asString.substring(1), 16)
                )
            }

            "modify-btn-text" -> {
                val btnText = event.getValue("btn-text")
                val btnEmoji = event.getValue("btn-emoji")

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


    /* ------------------------------------- */
    fun onGuildLeave(event: GuildLeaveEvent) {
        jsonGuildManager[event.guild.idLong].delete()
    }
}
