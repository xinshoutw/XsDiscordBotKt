package tw.xinshou.discord.plugin.botinfo

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale


internal object BotInfo {
    private lateinit var messageTemplate: MessageTemplate

    internal fun init() {
        messageTemplate = MessageTemplate(
            langDir = Event.pluginContext.pluginDirectory.resolve("lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val memberCounts = event.jda.guilds.sumOf { it.memberCount }

        val substitutor = Substitutor().putAll(
            "member_counts" to "$memberCounts",
            "guild_counts" to "${event.jda.guilds.size}",
        )

        event.hook.editOriginal(
            messageTemplate.buildEdit(
                "bot-info",
                event.userLocale,
                substitutor,
            ).build()
        ).queue()
    }
}
