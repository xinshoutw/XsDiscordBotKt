package tw.xinshou.discord.plugin._example

import core.command.CommandHandler
import core.command.slashCommand
import core.config.ConfigLoader
import core.i18n.Localizer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin._example.command.guildCommands
import tw.xinshou.discord.plugin._example.config.ConfigSerializer


object Event : Plugin {
    override lateinit var config: PluginConfig

    internal lateinit var pluginContext: PluginContext
    internal lateinit var localizer: Localizer
    internal lateinit var pluginConfig: ConfigSerializer

    override fun PluginContext.onLoad() {
        pluginContext = this

        pluginConfig = ConfigLoader.load(
            file = pluginDirectory.resolve("config.yaml"),
            default = "/config.yaml",
        )

        if (!pluginConfig.enabled) {
            logger.warn("_Example is disabled.")
            return
        }

        fileGetter.export("lang/", pluginDirectory)

        localizer = Localizer(
            langDir = pluginDirectory.resolve("lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )

        _Example.init()
    }

    override fun PluginContext.onReload() {
        onLoad()
        _Example.init()
    }

    override fun commands(): List<CommandHandler> {
        if (!pluginConfig.enabled) return emptyList()
        return guildCommands(localizer).map { data ->
            slashCommand(data) { event ->
                _Example.onSlashCommandInteraction(event)
            }
        }
    }
}
