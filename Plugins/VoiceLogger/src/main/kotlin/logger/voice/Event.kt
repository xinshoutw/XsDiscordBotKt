package tw.xinshou.discord.plugin.logger.voice

import core.command.CommandHandler
import core.command.ComponentHandler
import core.command.componentHandler
import core.config.ConfigLoader
import core.i18n.Localizer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.logger.voice.command.guildCommands
import tw.xinshou.discord.plugin.logger.voice.config.ConfigSerializer
import java.io.File

object Event : ListenerAdapter(), Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var registerLocalizer: Localizer
    internal lateinit var placeholderLocalizer: Localizer
    internal lateinit var pluginDirectory: File

    override fun PluginContext.onLoad() {
        this@Event.pluginDirectory = pluginDirectory

        pluginConfig = ConfigLoader.load(
            File(pluginDirectory, "config.yaml"),
            "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("VoiceLogger is disabled.")
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

    override fun PluginContext.onReload() {
        onLoad()
        if (!pluginConfig.enabled) return
        VoiceLogger.reload()
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
                onButton = { event -> VoiceLogger.onButtonInteraction(event) }
                onEntitySelect = { event -> VoiceLogger.onEntitySelectInteraction(event) }
            }
        )
    }

    override fun onChannelUpdateVoiceStatus(event: ChannelUpdateVoiceStatusEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        VoiceLogger.onChannelUpdateVoiceStatus(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        VoiceLogger.onGuildVoiceUpdate(event)
    }
}
