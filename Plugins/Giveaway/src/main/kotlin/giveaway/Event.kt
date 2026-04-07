package tw.xinshou.discord.plugin.giveaway

import core.command.CommandHandler
import core.command.ComponentHandler
import core.command.componentHandler
import core.config.ConfigLoader
import core.i18n.Localizer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.giveaway.command.guildCommands
import tw.xinshou.discord.plugin.giveaway.config.ConfigSerializer
import java.io.File

object Event : ListenerAdapter(), Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var localizer: Localizer
    internal lateinit var pluginDirectory: File

    private fun resolveLocale(localeTag: String): DiscordLocale {
        return runCatching { DiscordLocale.from(localeTag) }
            .getOrDefault(DiscordLocale.CHINESE_TAIWAN)
    }

    override fun PluginContext.onLoad() {
        pluginDirectory = this.pluginDirectory
        Giveaway.stopAutoDrawScheduler()

        pluginConfig = ConfigLoader.load(File(pluginDirectory, "config.yaml"), "/config.yaml")

        if (!pluginConfig.enabled) {
            logger.warn("Giveaway is disabled.")
            return
        }

        val defaultLocale = resolveLocale(pluginConfig.defaultLocale)

        localizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = defaultLocale,
        )

        Giveaway.reload(defaultLocale)
        Giveaway.startAutoDrawScheduler(pluginConfig.autoDrawIntervalSeconds)
    }

    override fun PluginContext.onUnload() {
        Giveaway.stopAutoDrawScheduler()
    }

    override fun PluginContext.onReload() {
        onUnload()
        onLoad()
    }

    override fun commands(): List<CommandHandler> {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        if (!::localizer.isInitialized) return emptyList()
        return guildCommands(localizer)
    }

    override fun components(): List<ComponentHandler> {
        val prefix = config.componentPrefix
        if (prefix.isBlank()) return emptyList()

        return listOf(
            componentHandler(prefix) {
                onButton = { event -> Giveaway.onButtonInteraction(event) }
                onStringSelect = { event -> Giveaway.onStringSelectInteraction(event) }
                onEntitySelect = { event -> Giveaway.onEntitySelectInteraction(event) }
                onModal = { event -> Giveaway.onModalInteraction(event) }
            }
        )
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        Giveaway.onGuildLeave(event)
    }
}
