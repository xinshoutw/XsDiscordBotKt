package tw.xserver.plugin.logger.chat


import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.base.BotLoader.jdaBot
import tw.xserver.loader.localizations.LangManager
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.logger.chat.JsonManager.dataMap
import tw.xserver.plugin.logger.chat.command.guildCommands
import tw.xserver.plugin.logger.chat.lang.CmdFileSerializer
import tw.xserver.plugin.logger.chat.lang.CmdLocalizations
import tw.xserver.plugin.logger.chat.lang.PlaceholderLocalizations
import tw.xserver.plugin.logger.chat.lang.PlaceholderSerializer
import tw.xserver.plugin.logger.chat.serializer.MainConfigSerializer
import java.io.File


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEvent(true) {
    internal const val COMPONENT_PREFIX = "chat-logger@"
    internal val PLUGIN_DIR_FILE = File("plugins/ChatLogger")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal lateinit var config: MainConfigSerializer
        private set

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadAll()

        logger.info("ChatLogger loaded.")
    }

    override fun unload() {
        DbManager.disconnect()
        dataMap.clear()

        logger.info("ChatLogger unloaded.")
    }

    override fun reloadConfigFile() {
        fileGetter.readInputStream("config.yaml").use {
            config = Yaml().decodeFromStream<MainConfigSerializer>(it)
        }

        logger.info("Data file loaded successfully.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("lang")

        LangManager(
            PLUGIN_DIR_FILE,
            "register.yaml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )

        LangManager(
            PLUGIN_DIR_FILE,
            "placeholder.yaml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = PlaceholderSerializer::class,
            clazzLocalization = PlaceholderLocalizations::class
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "chat-logger setting")) return
        ChatLogger.onSlashCommandInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        ChatLogger.onEntitySelectInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        ChatLogger.onButtonInteraction(event)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild || event.author == jdaBot.selfUser) return
        ChatLogger.onMessageReceived(event)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (!event.isFromGuild || event.author == jdaBot.selfUser) return
        ChatLogger.onMessageUpdate(event)
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (!event.isFromGuild) return
        ChatLogger.onMessageDelete(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        ChatLogger.onGuildLeave(event)
    }
}
