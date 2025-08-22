package tw.xinshou.plugin.logger.voice


import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.localizations.StringLocalizer
import tw.xinshou.core.plugin.PluginEvent
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.logger.voice.command.CmdFileSerializer
import tw.xinshou.plugin.logger.voice.command.PlaceholderSerializer
import tw.xinshou.plugin.logger.voice.command.guildCommands


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEvent(true) {
    private lateinit var registerLocalizer: StringLocalizer<CmdFileSerializer>
    internal lateinit var placeholderLocalizer: StringLocalizer<PlaceholderSerializer>

    override fun load() {
        super.load()

        registerLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            fileName = "register.yaml",
        )

        placeholderLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = PlaceholderSerializer::class,
            fileName = "placeholder.yaml",
        )
    }

    override fun reload() {
        super.reload()

        registerLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            fileName = "register.yaml",
        )

        placeholderLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = PlaceholderSerializer::class,
            fileName = "placeholder.yaml",
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands(registerLocalizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "voice-logger setting")) return
        VoiceLogger.onSlashCommandInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        VoiceLogger.onEntitySelectInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        VoiceLogger.onButtonInteraction(event)
    }

    override fun onChannelUpdateVoiceStatus(event: ChannelUpdateVoiceStatusEvent) {
        VoiceLogger.onChannelUpdateVoiceStatus(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        VoiceLogger.onGuildVoiceUpdate(event)
    }
}
