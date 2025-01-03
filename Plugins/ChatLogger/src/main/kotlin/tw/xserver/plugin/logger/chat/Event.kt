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
import tw.xserver.plugin.logger.chat.command.getGuildCommands
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
    internal const val COMPONENT_PREFIX = "xs:chat-logger:v2:"
    internal val PLUGIN_DIR_FILE = File("./plugins/ChatLogger/")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal lateinit var config: MainConfigSerializer
        private set

    override fun load() {
        reloadAll()
    }

    override fun unload() {
        DbManager.disconnect()
        dataMap.clear()
    }

    override fun reloadConfigFile() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)

        fileGetter.readInputStream("./config.yml").use {
            config = Yaml().decodeFromStream<MainConfigSerializer>(it)
        }

        logger.info("Data file loaded successfully.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("./lang")

        LangManager(
            PLUGIN_DIR_FILE,
            "register.yml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            clazzLocalization = CmdLocalizations::class
        )

        LangManager(
            PLUGIN_DIR_FILE,
            "placeholder.yml",
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = PlaceholderSerializer::class,
            clazzLocalization = PlaceholderLocalizations::class
        )
    }

    override fun guildCommands(): Array<CommandData> = getGuildCommands()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandName(event, "chat-logger setting")) return

        ChatLogger.setting(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkPrefix(event, COMPONENT_PREFIX)) return

        ChatLogger.onSelect(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkPrefix(event, COMPONENT_PREFIX)) return
        when (val componentId = event.componentId.removePrefix(COMPONENT_PREFIX)) {
            "toggle" -> ChatLogger.onToggle(event)
            "modify-allow", "modify-block" -> ChatLogger.createSel(
                event,
                componentId
            )

            "delete" -> ChatLogger.onDelete(event)
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild || event.author == jdaBot.selfUser) return

        ChatLogger.receiveMessage(event)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (!event.isFromGuild || event.author == jdaBot.selfUser) return

        ChatLogger.updateMessage(event)
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (!event.isFromGuild) return
        println("TRIGGER MESSAGE DELETED")
        println(event.messageId)
        ChatLogger.deleteMessage(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        ChatLogger.onGuildLeave(event)
    }
}
