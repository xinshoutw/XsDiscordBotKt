package tw.xinshou.discord.plugin._example

import tw.xinshou.discord.core.i18n.MessageTemplate
import tw.xinshou.discord.core.placeholder.Substitutor
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale


internal object _Example {
    private lateinit var messageTemplate: MessageTemplate

    internal fun init() {
        messageTemplate = MessageTemplate(
            langDir = Event.pluginContext.pluginDirectory.resolve("lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val option1 = event.getOption("option1")!!.asString
        val option2 = event.getOption("option2")?.asString

        val substitutor = Substitutor().putAll(
            "_Example@option1" to option1,
            "_Example@option2" to (option2 ?: "none"),
        )

        event.hook.editOriginal(
            messageTemplate.buildEdit(
                "example",
                event.userLocale,
                substitutor,
            ).build()
        ).queue()
    }
}
