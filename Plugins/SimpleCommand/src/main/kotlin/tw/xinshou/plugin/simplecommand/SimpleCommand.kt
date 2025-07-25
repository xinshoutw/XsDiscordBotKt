package tw.xinshou.plugin.simplecommand

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.loader.builtin.messagecreator.MessageCreator
import tw.xinshou.plugin.simplecommand.Event.PLUGIN_DIR_FILE
import java.io.File

internal object SimpleCommand {
    private val creator = MessageCreator(
        File(PLUGIN_DIR_FILE, "lang"),
        DiscordLocale.CHINESE_TAIWAN
    )

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "ctcb-none-card" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.getCreateBuilder("ctcb-none-card").build()
                ).queue()
            }

            "ctcb-remit" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.getCreateBuilder("ctcb-remit").build()
                ).queue()
            }

            "chpytwtp-remit" -> {
                event.hook.deleteOriginal().queue()
                event.channel.sendMessage(
                    creator.getCreateBuilder("chpytwtp-remit").build()
                ).queue()
            }
        }
    }
}