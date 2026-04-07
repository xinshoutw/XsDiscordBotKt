package tw.xinshou.discord.plugin.ticket.create

import core.placeholder.Substitutor
import core.placeholder.withUser
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import tw.xinshou.discord.plugin.ticket.Ticket.componentId
import tw.xinshou.discord.plugin.ticket.Ticket.jsonGuildManager
import tw.xinshou.discord.plugin.ticket.Ticket.messageTemplate

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
        steps[event.user.idLong] = step
    }

    private fun onAddTicket(event: SlashCommandInteractionEvent) {
        val messageId = event.getOption("message_id")!!.asLong
        event.messageChannel.retrieveMessageById(messageId)
            .onErrorFlatMap { event.hook.editOriginal("Cannot found the message by id: $messageId") }
            .flatMap {
                val firstActionRow = it.components.firstOrNull { c -> c.type == Component.Type.ACTION_ROW }?.asActionRow()
                if (firstActionRow != null && firstActionRow.components.size == 5) {
                    event.hook.editOriginal("This message is full of buttons, please recreate a new message")
                } else {
                    val step = Step(event.hook, it)
                    steps[event.user.idLong] = step
                    step.renderEmbedAction(event.userLocale)
                }
            }.queue()
    }

    fun onButtonInteraction(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        when (idMap["sub_action"]) {
            "prev" -> { val step = steps[event.user.idLong] ?: return; showReasonModal(event, step) }
            "modify-author" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); showAuthorModal(event, step) }
            "modify-content" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); showContentModal(event, step) }
            "modify-category" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); event.deferEdit().flatMap { step.hook.editOriginal(messageTemplate.buildEdit("modify-category", event.userLocale).build()) }.queue() }
            "modify-color" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); showColorModal(event, step) }
            "modify-btn-text" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); showBtnTextModal(event, step) }
            "modify-btn-color" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); event.deferEdit().flatMap { it.editOriginal(messageTemplate.buildEdit("modify-btn-color", event.userLocale).build()) }.queue() }
            "modify-reason" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); showReasonTitleModal(event, step) }
            "modify-admin" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); event.deferEdit().flatMap { step.hook.editOriginal(messageTemplate.buildEdit("modify-admin-role", event.userLocale).build()) }.queue() }
            "confirm-create" -> confirmCreate(event)
            "back" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); event.deferEdit().flatMap { step.renderEmbedAction(event.userLocale) }.queue() }
            "modify-btn-color-submit" -> { val step = steps[event.user.idLong] ?: return event.deferEdit().queue(); step.setBtnStyle(ButtonStyle.fromKey((idMap["color_index"] as String).toInt())); event.deferEdit().flatMap { step.renderEmbedAction(event.userLocale) }.queue() }
        }
    }

    private fun showReasonModal(event: ButtonInteractionEvent, step: Step) {
        val modalId = componentId.build("action" to "create", "sub_action" to "preview-reason")
        event.replyModal(Modal.create(modalId, "Preview Reason").addActionRow(TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH).setValue(step.data.json.reasonTitle).build()).build()).queue()
    }

    private fun showAuthorModal(event: ButtonInteractionEvent, step: Step) {
        val modalId = componentId.build("action" to "create", "sub_action" to "modify-author")
        event.replyModal(Modal.create(modalId, "Modify Author").addActionRow(TextInput.create("author", "Author", TextInputStyle.SHORT).setValue(step.data.author ?: "").setRequired(false).build()).addActionRow(TextInput.create("image", "Icon URL", TextInputStyle.SHORT).setValue(step.data.authorIconUrl ?: "").setRequired(false).build()).build()).queue()
    }

    private fun showContentModal(event: ButtonInteractionEvent, step: Step) {
        val modalId = componentId.build("action" to "create", "sub_action" to "modify-content")
        event.replyModal(Modal.create(modalId, "Modify Content").addActionRow(TextInput.create("title", "Title", TextInputStyle.SHORT).setValue(step.data.title ?: "").setRequired(false).build()).addActionRow(TextInput.create("desc", "Description", TextInputStyle.PARAGRAPH).setValue(step.data.description ?: "").setRequired(false).build()).build()).queue()
    }

    private fun showColorModal(event: ButtonInteractionEvent, step: Step) {
        val modalId = componentId.build("action" to "create", "sub_action" to "modify-embed-color")
        event.replyModal(Modal.create(modalId, "Modify Color").addActionRow(TextInput.create("color", "Hex Color (#RRGGBB)", TextInputStyle.SHORT).setValue(String.format("#%06X", step.data.color and 0xFFFFFF)).build()).build()).queue()
    }

    private fun showBtnTextModal(event: ButtonInteractionEvent, step: Step) {
        val modalId = componentId.build("action" to "create", "sub_action" to "modify-btn-text")
        event.replyModal(Modal.create(modalId, "Modify Button").addActionRow(TextInput.create("btn-text", "Button Text", TextInputStyle.SHORT).setValue(step.data.btnText ?: "").setRequired(false).build()).addActionRow(TextInput.create("btn-emoji", "Button Emoji", TextInputStyle.SHORT).setValue(step.data.btnEmoji?.asReactionCode ?: "").setRequired(false).build()).build()).queue()
    }

    private fun showReasonTitleModal(event: ButtonInteractionEvent, step: Step) {
        val modalId = componentId.build("action" to "create", "sub_action" to "modify-reason")
        event.replyModal(Modal.create(modalId, "Modify Reason").addActionRow(TextInput.create("reason", "Reason Title", TextInputStyle.SHORT).setValue(step.data.json.reasonTitle).build()).build()).queue()
    }

    private fun confirmCreate(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
        val manager = jsonGuildManager[event.guild!!.idLong]
        step.confirmCreateAction(event.userLocale, event.channel).map {
            val tmp = manager.data[it.id]
            if (tmp == null) manager.data[it.id] = mutableListOf(step.json)
            else tmp.add(step.json)
            manager.save()
        }.queue()
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
        val idMap = componentId.parse(event.componentId)
        when (idMap["sub_action"]) {
            "modify-admin" -> step.setAdminIds(event.values.map { it.idLong })
            "modify-category" -> { val categoryId = event.values[0].idLong; step.setCategoryId(categoryId) }
        }
        event.deferEdit().flatMap { step.renderEmbedAction(event.userLocale) }.queue()
    }

    fun onModalInteraction(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
        when (idMap["sub_action"]) {
            "modify-author" -> step.setAuthor(event.getValue("author")?.asString, event.getValue("image")?.asString)
            "modify-content" -> { step.setTitle(event.getValue("title")?.asString); step.setDesc(event.getValue("desc")?.asString) }
            "modify-reason" -> step.setReason(event.getValue("reason")!!.asString)
            "modify-embed-color" -> step.setColor(Integer.parseInt(event.getValue("color")!!.asString.substring(1), 16))
            "modify-btn-text" -> {
                val btnText = event.getValue("btn-text"); val btnEmoji = event.getValue("btn-emoji")
                if (btnText == null && btnEmoji == null) { event.reply("Either ButtonText or ButtonEmoji must be provided!").setEphemeral(true).queue(); return }
                step.setBtnContent(btnText?.asString)
                step.setBtnEmoji(btnEmoji?.let { if (it.asString.isNotEmpty()) Emoji.fromUnicode(it.asString) else null })
            }
        }
        event.deferEdit().flatMap { step.renderEmbedAction(event.userLocale) }.queue()
    }

    fun onGuildLeave(event: GuildLeaveEvent) { jsonGuildManager[event.guild.idLong].delete() }
}
