package tw.xinshou.discord.plugin.logger.voice


import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.logger.voice.command.CmdFileSerializer
import tw.xinshou.discord.plugin.logger.voice.command.PlaceholderSerializer
import tw.xinshou.discord.plugin.logger.voice.command.guildCommands
import tw.xinshou.discord.plugin.logger.voice.config.ConfigSerializer


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var registerLocalizer: StringLocalizer<CmdFileSerializer>
    internal lateinit var placeholderLocalizer: StringLocalizer<PlaceholderSerializer>

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("VoiceLogger is disabled.")
            return
        }

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

        if (!config.enabled) {
            logger.warn("VoiceLogger is disabled.")
            return
        }

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

        VoiceLogger.reload()
    }

    override fun guildCommands(): Array<CommandData> {
        return if (!config.enabled) {
            emptyArray()
        } else {
            guildCommands(registerLocalizer)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkCommandString(event, "voice-logger setting")) return
        VoiceLogger.onSlashCommandInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        VoiceLogger.onEntitySelectInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        VoiceLogger.onButtonInteraction(event)
    }

    override fun onChannelUpdateVoiceStatus(event: ChannelUpdateVoiceStatusEvent) {
        if (!config.enabled) return
        VoiceLogger.onChannelUpdateVoiceStatus(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (!config.enabled) return
        VoiceLogger.onGuildVoiceUpdate(event)
    }
}
