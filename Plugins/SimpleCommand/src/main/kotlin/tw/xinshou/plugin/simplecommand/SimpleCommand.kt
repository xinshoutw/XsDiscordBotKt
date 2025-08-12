package tw.xinshou.plugin.simplecommand

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.loader.builtin.messagecreator.MessageCreator
import tw.xinshou.plugin.simplecommand.Event.pluginDirectory

internal object SimpleCommand {
    private val creator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN
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