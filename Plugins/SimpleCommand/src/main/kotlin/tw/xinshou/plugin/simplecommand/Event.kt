package tw.xinshou.plugin.simplecommand

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.localizations.StringLocalizer
import tw.xinshou.core.plugin.PluginEvent
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.simplecommand.command.CmdFileSerializer
import tw.xinshou.plugin.simplecommand.command.commandStringSet
import tw.xinshou.plugin.simplecommand.command.guildCommands

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

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        SimpleCommand.onSlashCommandInteraction(event)
    }

    override fun guildCommands(): Array<CommandData> = guildCommands(localizer)
}
