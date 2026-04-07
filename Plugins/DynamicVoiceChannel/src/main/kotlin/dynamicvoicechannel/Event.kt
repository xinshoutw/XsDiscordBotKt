package tw.xinshou.discord.plugin.dynamicvoicechannel

import core.command.CommandHandler
import core.config.ConfigLoader
import core.i18n.Localizer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.dynamicvoicechannel.command.guildCommands
import tw.xinshou.discord.plugin.dynamicvoicechannel.config.ConfigSerializer
import java.io.File

internal object Event : ListenerAdapter(), Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var localizer: Localizer
    internal lateinit var pluginDirectory: File

    override fun PluginContext.onLoad() {
        this@Event.pluginDirectory = pluginDirectory
        pluginConfig = ConfigLoader.load(File(pluginDirectory, "config.yaml"), "/config.yaml")

        if (!pluginConfig.enabled) {
            logger.warn("DynamicVoiceChannel is disabled.")
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
        DynamicVoiceChannel.reload()
    }

    override fun commands(): List<CommandHandler> {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands(localizer)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        DynamicVoiceChannel.onGuildLeave(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        DynamicVoiceChannel.onGuildVoiceUpdate(event)
    }
}
