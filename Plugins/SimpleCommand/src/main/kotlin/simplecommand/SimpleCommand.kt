package simplecommand

import core.i18n.MessageTemplate
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import java.io.File

internal object SimpleCommand {
    private var creator: MessageTemplate
        get() = Event.messageTemplate
        set(_) {}

    internal fun reload() {
        // messageTemplate is reloaded in Event.onLoad()
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "cub-none-card" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.buildCreate("cub-none-card").build()
                ).queue()
            }

            "tsib-none-card" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.buildCreate("tsib-none-card").build()
                ).queue()
            }

            "ctcb-none-card" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.buildCreate("ctcb-none-card").build()
                ).queue()
            }

            "ctcb-remit" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.buildCreate("ctcb-remit").build()
                ).queue()
            }

            "chpytwtp-remit" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.buildCreate("chpytwtp-remit").build()
                ).queue()
            }
        }
    }
}
