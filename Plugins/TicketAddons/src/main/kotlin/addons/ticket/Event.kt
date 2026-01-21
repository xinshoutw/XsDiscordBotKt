package tw.xinshou.discord.plugin.addons.ticket

import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.plugin.addons.ticket.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    override fun load() {
        super.load()
        if (!config.enabled) {
            logger.warn("TicketAddons is disabled.")
            return
        }
    }

    override fun reload() {
        super.reload()
        if (!config.enabled) {
            logger.warn("TicketAddons is disabled.")
            return
        }

        TicketAddons.reload()
    }

    override fun onChannelCreate(event: ChannelCreateEvent) {
        if (!config.enabled) return
        TicketAddons.onChannelCreate(event)
    }
}
