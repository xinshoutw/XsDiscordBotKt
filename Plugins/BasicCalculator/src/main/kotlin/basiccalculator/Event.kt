package tw.xinshou.discord.plugin.basiccalculator

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.basiccalculator.command.CmdFileSerializer
import tw.xinshou.discord.plugin.basiccalculator.command.guildCommands
import tw.xinshou.discord.plugin.basiccalculator.config.ConfigSerializer


object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("BasicCalculator is disabled.")
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
            logger.warn("BasicCalculator is disabled.")
            return
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        BasicCalculator.reload()
    }

    override fun guildCommands(): Array<CommandData> {
        return if (!config.enabled) {
            emptyArray()
        } else {
            guildCommands(localizer)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkCommandString(event, "basic-calculate")) return
        BasicCalculator.onSlashCommandInteraction(event)
    }
}
