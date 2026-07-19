package tw.xinshou.discord.plugin.botinfo

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.config.ConfigLoader
import tw.xinshou.discord.core.i18n.Localizer
import tw.xinshou.discord.core.plugin.Plugin
import tw.xinshou.discord.core.plugin.PluginConfig
import tw.xinshou.discord.core.plugin.PluginContext
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
