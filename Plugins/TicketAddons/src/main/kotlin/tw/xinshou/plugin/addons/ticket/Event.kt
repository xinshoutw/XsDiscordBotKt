package tw.xinshou.plugin.addons.ticket

import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import tw.xinshou.loader.plugin.PluginEventConfigure
import tw.xinshou.plugin.addons.ticket.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    override fun onChannelCreate(event: ChannelCreateEvent) {
        TicketAddons.onChannelCreate(event)
    }
}
