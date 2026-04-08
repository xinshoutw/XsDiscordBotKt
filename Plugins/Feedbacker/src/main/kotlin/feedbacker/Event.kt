package tw.xinshou.discord.plugin.feedbacker

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.ComponentHandler
import tw.xinshou.discord.core.command.componentHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.config.ConfigLoader
import tw.xinshou.discord.core.i18n.Localizer
import tw.xinshou.discord.core.plugin.Plugin
import tw.xinshou.discord.core.plugin.PluginConfig
import tw.xinshou.discord.core.plugin.PluginContext
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.feedbacker.command.guildCommands
import tw.xinshou.discord.plugin.feedbacker.config.ConfigSerializer
import java.io.File

object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var localizer: Localizer
    internal lateinit var pluginDirectory: File

    override fun PluginContext.onLoad() {
        this@Event.pluginDirectory = pluginDirectory

        pluginConfig = ConfigLoader.load(
            File(pluginDirectory, "config.yaml"),
            "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("Feedbacker is disabled.")
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

        Feedbacker.reload()
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
                onButton = { event -> Feedbacker.onButtonInteraction(event) }
                onModal = { event -> Feedbacker.onModalInteraction(event) }
            }
        )
    }
}
