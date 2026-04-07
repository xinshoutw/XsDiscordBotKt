package addons.ticket

import core.config.ConfigLoader
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import addons.ticket.config.ConfigSerializer
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.io.File

object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")

    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var pluginDirectory: File

    private val jdaListener = object : ListenerAdapter() {
        override fun onChannelCreate(event: ChannelCreateEvent) {
            if (!pluginConfig.enabled) return
            TicketAddons.onChannelCreate(event)
        }
    }

    override fun PluginContext.onLoad() {
        this@Event.pluginDirectory = pluginDirectory
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("TicketAddons is disabled.")
            return
        }
    }

    override fun PluginContext.onReload() {
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("TicketAddons is disabled.")
            return
        }

        TicketAddons.reload()
    }

    override fun listeners(): List<Any> = listOf(jdaListener)
}
