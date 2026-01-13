package tw.xinshou.plugin.dynamicvoicechannel


import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.localizations.StringLocalizer
import tw.xinshou.core.plugin.PluginEventConfigure
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.dynamicvoicechannel.command.CmdFileSerializer
import tw.xinshou.plugin.dynamicvoicechannel.command.commandStringSet
import tw.xinshou.plugin.dynamicvoicechannel.command.guildCommands
import tw.xinshou.plugin.dynamicvoicechannel.config.ConfigSerializer


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
internal object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("DynamicVoiceChannel is disabled.")
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
            logger.warn("DynamicVoiceChannel is disabled.")
            return
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        DynamicVoiceChannel.reload()
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
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        DynamicVoiceChannel.onSlashCommandInteraction(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!config.enabled) return
        DynamicVoiceChannel.onGuildLeave(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (!config.enabled) return
        DynamicVoiceChannel.onGuildVoiceUpdate(event)
    }
}
