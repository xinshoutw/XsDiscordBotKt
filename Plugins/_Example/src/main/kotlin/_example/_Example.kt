package tw.xinshou.discord.plugin._example

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.discord.core.builtin.messagecreator.MessageCreator
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.plugin._example.Event.pluginDirectory


internal object _Example {
    private var messageCreator = MessageCreator(
        pluginDirectory,
        DiscordLocale.CHINESE_TAIWAN
    )

    internal fun reload() {
        messageCreator = MessageCreator(
            pluginDirectory,
            DiscordLocale.CHINESE_TAIWAN
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val option1 = event.getOption("option1")!!.asString
        val option2 = event.getOption("option2")?.asString

        Placeholder.get(event).putAll(
            "_Example@option1" to option1,
            "_Example@option2" to (option2 ?: "none")
        )

        event.hook.editOriginal(
            MessageEditData.fromCreateData(
                messageCreator.getCreateBuilder(
                    "example",
                    event.userLocale,
                    Placeholder.get(event)
                ).build(),
            )
        ).queue()
    }
}

