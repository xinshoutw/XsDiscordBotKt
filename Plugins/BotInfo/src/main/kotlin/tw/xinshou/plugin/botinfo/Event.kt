package tw.xinshou.plugin.botinfo

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.loader.localizations.StringLocalizer
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.botinfo.command.CmdFileSerializer
import tw.xinshou.plugin.botinfo.command.globalCommands

object Event : PluginEvent(true) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun reload() {
        super.reload()

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun globalCommands(): Array<CommandData> = globalCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "bot-info")) return
        BotInfo.onSlashCommandInteraction(event)
    }
}