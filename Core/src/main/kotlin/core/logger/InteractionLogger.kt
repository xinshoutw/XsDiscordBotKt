package tw.xinshou.discord.core.logger

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.util.Arguments

/**
 * This class handles logging of different types of interaction events on Discord.
 * It extends ListenerAdapter to respond to various interaction events such as commands and button presses.
 */
internal object InteractionLogger : ListenerAdapter() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Logs slash command interactions. It defers the reply to the interaction and logs the command.
     * @param event SlashCommandInteractionEvent provided by JDA when a slash command is executed.
     */
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (Arguments.autoDeferInteractionReplies && !event.isAcknowledged) {
            event.deferReply(true).queue()
        }
        logger.info("[CMD] {}: {}", event.user.name, event.commandString)
    }

    /**
     * Logs message context interactions. It defers the reply to the interaction and logs the command.
     * @param event MessageContextInteractionEvent provided by JDA when a context menu item is used on a message.
     */
    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
        if (Arguments.autoDeferInteractionReplies && !event.isAcknowledged) {
            event.deferReply(true).queue()
        }
        logger.info("[MSG] {}: {}", event.user.name, event.commandString)
    }

    /**
     * Logs button click interactions.
     * @param event ButtonInteractionEvent provided by JDA when a button within an interaction is clicked.
     */
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        logger.info("[BTN] {}: {}", event.user.name, event.componentId)
    }

    /**
     * Logs modal interactions. This is triggered when a user submits a modal dialog form.
     * @param event ModalInteractionEvent provided by JDA when a modal dialog is interacted with.
     */
    override fun onModalInteraction(event: ModalInteractionEvent) {
        logger.info("[MODAL] {}: {}", event.user.name, event.modalId)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        logger.info("[ENTITY] {}: {}", event.user.name, event.componentId)
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        logger.info("[STRING] {}: {}", event.user.name, event.componentId)
    }
}
