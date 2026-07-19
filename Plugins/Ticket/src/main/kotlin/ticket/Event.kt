package tw.xinshou.discord.plugin.ticket

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.ComponentHandler
import tw.xinshou.discord.core.command.componentHandler
import tw.xinshou.discord.core.config.ConfigLoader
import tw.xinshou.discord.core.i18n.Localizer
import tw.xinshou.discord.core.plugin.Plugin
import tw.xinshou.discord.core.plugin.PluginConfig
import tw.xinshou.discord.core.plugin.PluginContext
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.ticket.command.guildCommands
import tw.xinshou.discord.plugin.ticket.config.ConfigSerializer
import java.io.File

object Event : ListenerAdapter(), Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var localizer: Localizer
    internal lateinit var pluginDirectory: File
    internal val logger get() = org.slf4j.LoggerFactory.getLogger(this::class.java)

    override fun PluginContext.onLoad() {
        this@Event.pluginDirectory = pluginDirectory
        pluginConfig = ConfigLoader.load(File(pluginDirectory, "config.yaml"), "/config.yaml")

        if (!pluginConfig.enabled) {
            logger.warn("Ticket is disabled.")
            return
        }

        localizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    override fun PluginContext.onReload() {
        onLoad()
        if (!pluginConfig.enabled) return
        Ticket.reload()
    }

    override fun commands(): List<CommandHandler> {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands(localizer)
    }

    override fun components(): List<ComponentHandler> {
        val prefix = config.componentPrefix
        if (prefix.isBlank()) return emptyList()

        return listOf(
            componentHandler(prefix) {
                onButton = { event -> Ticket.onButtonInteraction(event) }
                onEntitySelect = { event -> Ticket.onEntitySelectInteraction(event) }
                onModal = { event -> Ticket.onModalInteraction(event) }
            }
        )
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        Ticket.onGuildLeave(event)
    }
}
