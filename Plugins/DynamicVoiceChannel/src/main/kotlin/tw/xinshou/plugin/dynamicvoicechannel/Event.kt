package tw.xinshou.plugin.dynamicvoicechannel


import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.localizations.StringLocalizer
import tw.xinshou.core.plugin.PluginEvent
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.dynamicvoicechannel.command.CmdFileSerializer
import tw.xinshou.plugin.dynamicvoicechannel.command.commandStringSet
import tw.xinshou.plugin.dynamicvoicechannel.command.guildCommands


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
internal object Event : PluginEvent(true) {
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

    override fun guildCommands(): Array<CommandData> = guildCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        GlobalUtil.checkSlashCommand(event, commandStringSet)
        DynamicVoiceChannel.onSlashCommandInteraction(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        DynamicVoiceChannel.onGuildLeave(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        DynamicVoiceChannel.onGuildVoiceUpdate(event)
    }
}
