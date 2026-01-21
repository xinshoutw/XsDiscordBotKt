package tw.xinshou.discord.plugin.botinfo

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.botinfo.command.CmdFileSerializer
import tw.xinshou.discord.plugin.botinfo.command.globalCommands
import tw.xinshou.discord.plugin.botinfo.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("BotInfo is disabled.")
            return
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            logger.warn("BotInfo is disabled.")
            return
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        BotInfo.reload()
    }

    override fun globalCommands(): Array<CommandData> {
        return if (!config.enabled) {
            emptyArray()
        } else {
            globalCommands(localizer)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkCommandString(event, "bot-info")) return
        BotInfo.onSlashCommandInteraction(event)
    }
}