package tw.xinshou.plugin.logger.voice


import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.localizations.DiscordLocalizationExporter
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.logger.voice.command.guildCommands
import tw.xinshou.plugin.logger.voice.command.lang.CmdFileSerializer
import tw.xinshou.plugin.logger.voice.command.lang.CmdLocalizations
import tw.xinshou.plugin.logger.voice.command.lang.PlaceholderLocalizations
import tw.xinshou.plugin.logger.voice.command.lang.PlaceholderSerializer
import java.io.File


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEvent(true) {
    internal const val COMPONENT_PREFIX = "voice-logger@"
    internal val PLUGIN_DIR_FILE = File("plugins/VoiceLogger")
    internal val DEFAULT_LOCALE = DiscordLocale.CHINESE_TAIWAN
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reload(true)

        logger.info("VoiceLogger loaded.")
    }

    override fun unload() {
        logger.info("VoiceLogger unloaded.")
    }

    override fun reload(init: Boolean) {
        fileGetter.exportDefaultDirectory("lang")

        DiscordLocalizationExporter(
            PLUGIN_DIR_FILE,
            "register.yaml",
            defaultLocale = DEFAULT_LOCALE,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )

        DiscordLocalizationExporter(
            PLUGIN_DIR_FILE,
            "placeholder.yaml",
            defaultLocale = DEFAULT_LOCALE,
            clazzSerializer = PlaceholderSerializer::class,
            clazzLocalization = PlaceholderLocalizations::class
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "voice-logger setting")) return
        VoiceLogger.onSlashCommandInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        VoiceLogger.onEntitySelectInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        VoiceLogger.onButtonInteraction(event)
    }

    override fun onChannelUpdateVoiceStatus(event: ChannelUpdateVoiceStatusEvent) {
        VoiceLogger.onChannelUpdateVoiceStatus(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        VoiceLogger.onGuildVoiceUpdate(event)
    }
}
