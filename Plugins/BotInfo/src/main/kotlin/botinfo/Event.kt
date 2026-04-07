package tw.xinshou.discord.plugin.botinfo

import core.command.CommandHandler
import core.command.slashCommand
import core.config.ConfigLoader
import core.i18n.Localizer
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.botinfo.command.globalCommands
import tw.xinshou.discord.plugin.botinfo.config.ConfigSerializer


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
            logger.warn("BotInfo is disabled.")
            return
        }

        fileGetter.export("lang/", pluginDirectory)

        localizer = Localizer(
            langDir = pluginDirectory.resolve("lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )

        BotInfo.init()
    }

    override fun PluginContext.onReload() {
        onLoad()
        BotInfo.init()
    }

    override fun commands(): List<CommandHandler> {
        if (!pluginConfig.enabled) return emptyList()
        return globalCommands(localizer).map { data ->
            slashCommand(data, isGlobal = true) { event ->
                BotInfo.onSlashCommandInteraction(event)
            }
        }
    }
}
