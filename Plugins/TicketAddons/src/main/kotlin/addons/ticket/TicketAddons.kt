package tw.xinshou.discord.plugin.addons.ticket

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.core.builtin.messagecreator.v2.MessageCreator
import tw.xinshou.discord.plugin.addons.ticket.Event.config
import tw.xinshou.discord.plugin.addons.ticket.Event.pluginDirectory
import java.util.concurrent.TimeUnit

internal object TicketAddons {
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

    fun onChannelCreate(event: ChannelCreateEvent) {
        if (!event.isFromGuild && event.guild.id != config.guildId) return
        if (config.prefix.none { event.channel.name.startsWith(it) }) return

        event.guild.retrieveMemberById(config.userId).queue {
            if (it.onlineStatus != OnlineStatus.OFFLINE) return@queue

            val delay: Long = config.delayMillis
            event.channel.asTextChannel().sendMessage(
                messageCreator.getCreateBuilder(
                    key = "not-online",
                    locale = DiscordLocale.CHINESE_TAIWAN
                ).build()
            ).queueAfter(delay, TimeUnit.MILLISECONDS)
        }
    }
}