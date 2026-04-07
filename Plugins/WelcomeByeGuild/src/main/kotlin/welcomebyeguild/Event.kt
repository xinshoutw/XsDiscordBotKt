package welcomebyeguild

import core.command.CommandHandler
import core.config.ConfigLoader
import core.i18n.Localizer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import welcomebyeguild.command.guildCommands
import welcomebyeguild.config.ConfigSerializer
import java.io.File

object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")

    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var pluginDirectory: File
    internal lateinit var localizer: Localizer

    private val jdaListener = object : ListenerAdapter() {
        override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
            if (!pluginConfig.enabled) return
            WelcomeByeGuild.onGuildMemberJoin(event)
        }

        override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
            if (!pluginConfig.enabled) return
            WelcomeByeGuild.onGuildMemberRemove(event)
        }

        override fun onGuildLeave(event: GuildLeaveEvent) {
            if (!pluginConfig.enabled) return
            WelcomeByeGuild.onGuildLeave(event)
        }
    }

    override fun PluginContext.onLoad() {
        pluginDirectory = this.pluginDirectory
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        localizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )

        if (!pluginConfig.enabled) {
            logger.warn("WelcomeByeGuild is disabled.")
            return
        }

        WelcomeByeGuild.load()
    }

    override fun PluginContext.onReload() {
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        localizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )

        if (!pluginConfig.enabled) {
            logger.warn("WelcomeByeGuild is disabled.")
            return
        }

        WelcomeByeGuild.reload()
    }

    override fun commands(): List<CommandHandler> {
        if (!this::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands(localizer)
    }

    override fun listeners(): List<Any> = listOf(jdaListener)
}
