package tw.xserver.plugin.logger.voice


import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.localizations.LangManager
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.logger.voice.command.guildCommands
import tw.xserver.plugin.logger.voice.lang.CmdFileSerializer
import tw.xserver.plugin.logger.voice.lang.CmdLocalizations
import tw.xserver.plugin.logger.voice.lang.PlaceholderLocalizations
import tw.xserver.plugin.logger.voice.lang.PlaceholderSerializer
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
        reloadAll()

        logger.info("VoiceLogger loaded.")
    }

    override fun unload() {
        logger.info("VoiceLogger unloaded.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("lang")

        LangManager(
            PLUGIN_DIR_FILE,
            "register.yml",
            defaultLocale = DEFAULT_LOCALE,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )

        LangManager(
            PLUGIN_DIR_FILE,
            "placeholder.yml",
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
