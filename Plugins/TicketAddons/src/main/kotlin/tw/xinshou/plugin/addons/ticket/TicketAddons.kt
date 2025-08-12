package tw.xinshou.plugin.addons.ticket

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.loader.builtin.messagecreator.MessageCreator
import tw.xinshou.plugin.addons.ticket.Event.config
import tw.xinshou.plugin.addons.ticket.Event.pluginDirectory
import java.util.concurrent.TimeUnit

internal object TicketAddons {
    private val creator = MessageCreator(
        pluginDirectory,
        DiscordLocale.CHINESE_TAIWAN
    )

    fun onChannelCreate(event: ChannelCreateEvent) {
        if (!event.isFromGuild && event.guild.id != config.guildId) return
        if (config.prefix.none { event.channel.name.startsWith(it) }) return

        event.guild.retrieveMemberById(config.userId).queue {
            if (it.onlineStatus != OnlineStatus.OFFLINE) return@queue

            val delay: Long = config.delayMillis
            event.channel.asTextChannel().sendMessage(
                creator.getCreateBuilder(
                    key = "not-online",
                    locale = DiscordLocale.CHINESE_TAIWAN
                ).build()
            ).queueAfter(delay, TimeUnit.MILLISECONDS)
        }
    }
}