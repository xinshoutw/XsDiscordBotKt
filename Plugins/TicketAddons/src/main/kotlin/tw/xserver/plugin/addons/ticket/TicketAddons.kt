package tw.xserver.plugin.addons.ticket

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xserver.loader.builtin.messagecreator.MessageCreator
import tw.xserver.plugin.addons.ticket.Event.PLUGIN_DIR_FILE
import tw.xserver.plugin.addons.ticket.Event.config
import java.io.File
import java.util.concurrent.TimeUnit

internal object TicketAddons {
    private val creator = MessageCreator(
        File(PLUGIN_DIR_FILE, "lang"),
        DiscordLocale.CHINESE_TAIWAN,
        listOf(
            "not-online"
        )
    )

    fun onChannelCreate(event: ChannelCreateEvent) {
        if (!event.isFromGuild && event.guild.id != config.guildId) return
        if (!event.channel.name.startsWith(config.prefix)) return

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