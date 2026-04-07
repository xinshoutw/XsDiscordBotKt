package tw.xinshou.discord.plugin.logger.chat

import core.command.CommandHandler
import core.command.ComponentHandler
import core.command.componentHandler
import core.config.ConfigLoader
import core.i18n.Localizer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.logger.chat.command.guildCommands
import tw.xinshou.discord.plugin.logger.chat.config.ConfigSerializer
import java.io.File

object Event : ListenerAdapter(), Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var registerLocalizer: Localizer
    internal lateinit var placeholderLocalizer: Localizer
    internal lateinit var pluginDirectory: File

    override fun PluginContext.onLoad() {
        pluginDirectory = this.pluginDirectory

        pluginConfig = ConfigLoader.load(
            File(pluginDirectory, "config.yaml"),
            "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("ChatLogger is disabled.")
            return
        }

        registerLocalizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )

        placeholderLocalizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    override fun PluginContext.onUnload() {
        DbManager.disconnect()
        JsonManager.dataMap.clear()
        logger.info("ChatLogger unloaded.")
    }

    override fun PluginContext.onReload() {
        onUnload()
        onLoad()

        if (!pluginConfig.enabled) return

        ChatLogger.reload()
    }

    override fun commands(): List<CommandHandler> {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands(registerLocalizer)
    }

    override fun components(): List<ComponentHandler> {
        val prefix = config.componentPrefix
        if (prefix.isBlank()) return emptyList()

        return listOf(
            componentHandler(prefix) {
                onButton = { event -> ChatLogger.onButtonInteraction(event) }
                onEntitySelect = { event -> ChatLogger.onEntitySelectInteraction(event) }
            }
        )
    }

    // Raw JDA events for message tracking
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        if (!event.isFromGuild || event.author.isBot) return
        ChatLogger.onMessageReceived(event)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        if (!event.isFromGuild || event.author.isBot) return
        ChatLogger.onMessageUpdate(event)
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        if (!event.isFromGuild) return
        ChatLogger.onMessageDelete(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        ChatLogger.onGuildLeave(event)
    }
}
