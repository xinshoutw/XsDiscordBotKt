package simplecommand

import core.command.CommandHandler
import core.command.slashCommand
import core.config.ConfigLoader
import core.i18n.Localizer
import core.i18n.MessageTemplate
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.interactions.DiscordLocale
import simplecommand.command.guildCommands
import simplecommand.config.ConfigSerializer
import java.io.File

object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")

    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var localizer: Localizer
    internal lateinit var messageTemplate: MessageTemplate
    private lateinit var ctx: PluginContext

    override fun PluginContext.onLoad() {
        ctx = this
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("SimpleCommand is disabled.")
            return
        }

        localizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )

        messageTemplate = MessageTemplate(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    override fun PluginContext.onReload() {
        onLoad()
        if (pluginConfig.enabled) {
            SimpleCommand.reload()
        }
    }

    override fun commands(): List<CommandHandler> {
        if (!this::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands(localizer)
    }
}
