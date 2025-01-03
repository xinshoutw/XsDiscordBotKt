package tw.xserver.plugin.logger.voice


import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
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
import tw.xserver.plugin.logger.voice.command.getGuildCommands
import tw.xserver.plugin.logger.voice.lang.CmdFileSerializer
import tw.xserver.plugin.logger.voice.lang.CmdLocalizations
import tw.xserver.plugin.logger.voice.lang.PlaceholderLocalizations
import tw.xserver.plugin.logger.voice.lang.PlaceholderSerializer
import java.io.File


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEvent(true) {
    internal const val COMPONENT_PREFIX = "xs:voice-logger:v2:"
    internal val PLUGIN_DIR_FILE = File("./plugins/VoiceLogger/")
    internal val DEFAULT_LOCALE = DiscordLocale.CHINESE_TAIWAN
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    override fun load() {
        reloadAll()
    }

    override fun unload() {}

    override fun reloadConfigFile() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)

        logger.info("Data file loaded successfully.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("./lang")

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

    override fun guildCommands(): Array<CommandData> = getGuildCommands()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandName(event, "voice-logger setting")) return

        VoiceLogger.setting(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkPrefix(event, COMPONENT_PREFIX)) return

        VoiceLogger.onSelect(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkPrefix(event, COMPONENT_PREFIX)) return
        when (val componentId = event.componentId.removePrefix(COMPONENT_PREFIX)) {
            "toggle" -> VoiceLogger.onToggle(event)
            "modify-allow", "modify-block" -> VoiceLogger.createSel(event, componentId)
            "delete" -> VoiceLogger.onDelete(event)
        }
    }

    override fun onChannelUpdateVoiceStatus(event: ChannelUpdateVoiceStatusEvent) {
        val guildId = event.guild.idLong
        val locale =
            if (!event.guild.features.contains("COMMUNITY")) DEFAULT_LOCALE
            else event.guild.locale
        val channel: VoiceChannel = event.channel.asVoiceChannel()
        val entryList = event.guild.retrieveAuditLogs()
            .limit(1)
            .type(ActionType.VOICE_CHANNEL_STATUS_UPDATE)
            .complete()
        val member = event.guild.retrieveMemberById(entryList[0].userIdLong)
        val oldStr = event.oldValue
        val newStr = event.newValue
        val data = VoiceLogger.StatusEventData(guildId, locale, channel, member, oldStr, newStr)

        if (newStr!!.isEmpty()) {
            VoiceLogger.onChannelStatusDelete(event, data)
        } else if (oldStr!!.isEmpty()) {
            VoiceLogger.onChannelStatusNew(event, data)
        } else {
            VoiceLogger.onChannelStatusUpdate(event, data)
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val guildId = event.guild.idLong
        val locale =
            if (!event.guild.features.contains("COMMUNITY")) DEFAULT_LOCALE
            else event.guild.locale
        val member = event.member
        val channelJoin = event.channelJoined
        val channelLeft = event.channelLeft
        val data = VoiceLogger.VoiceEventData(guildId, locale, member, channelJoin, channelLeft)

        if (channelJoin == null) {
            VoiceLogger.onChannelLeft(event, data)
        } else if (channelLeft == null) {
            VoiceLogger.onChannelJoin(event, data)
        } else {
            VoiceLogger.onChannelSwitch(event, data)
        }
    }
}
