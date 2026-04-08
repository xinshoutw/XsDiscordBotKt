package tw.xinshou.discord.plugin.addons.ticket

import tw.xinshou.discord.core.i18n.MessageTemplate
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import java.io.File
import java.util.concurrent.TimeUnit

internal object TicketAddons {
    private var messageTemplate = MessageTemplate(
        langDir = File(Event.pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
    )

    internal fun reload() {
        messageTemplate = MessageTemplate(
            langDir = File(Event.pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    fun onChannelCreate(event: ChannelCreateEvent) {
        val config = Event.pluginConfig
        if (!event.isFromGuild && event.guild.id != config.guildId) return
        if (config.prefix.none { event.channel.name.startsWith(it) }) return

        event.guild.retrieveMemberById(config.userId).queue {
            if (it.onlineStatus != OnlineStatus.OFFLINE) return@queue

            val delay: Long = config.delayMillis
            event.channel.asTextChannel().sendMessage(
                messageTemplate.buildCreate(
                    messageId = "not-online",
                    locale = DiscordLocale.CHINESE_TAIWAN
                ).build()
            ).queueAfter(delay, TimeUnit.MILLISECONDS)
        }
    }
}
