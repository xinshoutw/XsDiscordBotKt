package tw.xserver.plugin.feedbacker

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.base.BotLoader.jdaBot
import tw.xserver.loader.localizations.LangManager
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.feedbacker.command.guildCommands
import tw.xserver.plugin.feedbacker.lang.CmdFileSerializer
import tw.xserver.plugin.feedbacker.lang.CmdLocalizations
import tw.xserver.plugin.feedbacker.serializer.MainConfigSerializer
import java.io.File
import java.io.IOException

object Event : PluginEvent(true) {
    internal val PLUGIN_DIR_FILE = File("plugins/Feedbacker")
    internal const val COMPONENT_PREFIX = "feedbacker@"
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal lateinit var config: MainConfigSerializer
    internal lateinit var globalLocale: DiscordLocale

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadAll()

        logger.info("Feedbacker loaded.")
    }

    override fun unload() {
        logger.info("Feedbacker unloaded.")
    }

    override fun reloadConfigFile() {
        try {
            fileGetter.readInputStream("config.yaml").use {
                config = Yaml().decodeFromStream<MainConfigSerializer>(it)
            }
        } catch (e: IOException) {
            logger.error("Please configure {}./config.yaml!", PLUGIN_DIR_FILE.canonicalPath, e)
        }

        globalLocale = DiscordLocale.from(config.language)
        logger.info("Setting file loaded successfully.")

        jdaBot.getGuildById(config.guildId)?.let { guild ->
            Feedbacker.guild = guild
            Feedbacker.submitChannel = guild.getTextChannelById(config.submitChannelId)!!
        }
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
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "feedbacker")) return
        Feedbacker.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return
        Feedbacker.onButtonInteraction(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (GlobalUtil.checkModalIdPrefix(event, COMPONENT_PREFIX)) return
        Feedbacker.onModalInteraction(event)
    }
}
