package tw.xinshou.discord.core.logger

import tw.xinshou.discord.core.util.Arguments
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory

class InteractionLogger : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(InteractionLogger::class.java)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (Arguments.autoDeferInteractionReplies && !event.isAcknowledged) {
            event.deferReply(true).queue()
        }
        logger.info("[CMD] {}: /{}", event.user.name, event.fullCommandName)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (Arguments.autoDeferInteractionReplies && !event.isAcknowledged) {
            event.deferEdit().queue()
        }
        logger.info("[BTN] {}: {}", event.user.name, event.componentId)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (Arguments.autoDeferInteractionReplies && !event.isAcknowledged) {
            event.deferEdit().queue()
        }
        logger.info("[MODAL] {}: {}", event.user.name, event.modalId)
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (Arguments.autoDeferInteractionReplies && !event.isAcknowledged) {
            event.deferEdit().queue()
        }
        logger.info("[SSEL] {}: {}", event.user.name, event.componentId)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (Arguments.autoDeferInteractionReplies && !event.isAcknowledged) {
            event.deferEdit().queue()
        }
        logger.info("[ESEL] {}: {}", event.user.name, event.componentId)
    }
}
