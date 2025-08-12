package tw.xinshou.plugin.basiccalculator

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.loader.localizations.StringLocalizer
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.basiccalculator.command.CmdFileSerializer
import tw.xinshou.plugin.basiccalculator.command.guildCommands


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

    override fun guildCommands() = guildCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "basic-calculate")) return
        BasicCalculator.onSlashCommandInteraction(event)
    }
}
