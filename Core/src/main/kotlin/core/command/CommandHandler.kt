package tw.xinshou.discord.core.command

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData

data class CommandHandler(
    val data: CommandData,
    val isGlobal: Boolean = false,
    val execute: suspend (SlashCommandInteractionEvent) -> Unit,
)

data class ComponentHandler(
    val prefix: String,
    val onButton: (suspend (ButtonInteractionEvent) -> Unit)? = null,
    val onModal: (suspend (ModalInteractionEvent) -> Unit)? = null,
    val onStringSelect: (suspend (StringSelectInteractionEvent) -> Unit)? = null,
    val onEntitySelect: (suspend (EntitySelectInteractionEvent) -> Unit)? = null,
)

// DSL helpers for plugins

fun slashCommand(
    data: CommandData,
    isGlobal: Boolean = false,
    execute: suspend (SlashCommandInteractionEvent) -> Unit,
) = CommandHandler(data, isGlobal, execute)

fun componentHandler(
    prefix: String,
    builder: ComponentHandlerBuilder.() -> Unit,
): ComponentHandler {
    val b = ComponentHandlerBuilder(prefix)
    b.builder()
    return b.build()
}

class ComponentHandlerBuilder(private val prefix: String) {
    var onButton: (suspend (ButtonInteractionEvent) -> Unit)? = null
    var onModal: (suspend (ModalInteractionEvent) -> Unit)? = null
    var onStringSelect: (suspend (StringSelectInteractionEvent) -> Unit)? = null
    var onEntitySelect: (suspend (EntitySelectInteractionEvent) -> Unit)? = null

    fun build() = ComponentHandler(prefix, onButton, onModal, onStringSelect, onEntitySelect)
}
