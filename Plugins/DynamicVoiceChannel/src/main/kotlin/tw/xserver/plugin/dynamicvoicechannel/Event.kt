package tw.xserver.plugin.dynamicvoicechannel


import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.localizations.LangManager
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.dynamicvoicechannel.command.commandStringSet
import tw.xserver.plugin.dynamicvoicechannel.command.guildCommands
import tw.xserver.plugin.dynamicvoicechannel.lang.CmdFileSerializer
import tw.xserver.plugin.dynamicvoicechannel.lang.CmdLocalizations
import java.io.File


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
internal object Event : PluginEvent(true) {
    const val PLUGIN_NAME = "DynamicVoiceChannel"
    val PLUGIN_DIR_FILE = File("plugins/$PLUGIN_NAME")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadAll()

        logger.info("DynamicVoiceChannel loaded.")
    }

    override fun unload() {
        logger.info("DynamicVoiceChannel unloaded.")
    }

    override fun reloadConfigFile() {

        logger.info("Data file loaded successfully.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("lang")

        LangManager(
            pluginDirFile = PLUGIN_DIR_FILE,
            fileName = "register.yaml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

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
