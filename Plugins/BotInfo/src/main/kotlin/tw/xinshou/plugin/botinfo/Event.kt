package tw.xinshou.plugin.botinfo

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.localizations.LangManager
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.botinfo.command.guildCommands
import tw.xinshou.plugin.botinfo.lang.CmdFileSerializer
import tw.xinshou.plugin.botinfo.lang.CmdLocalizations
import java.io.File

object Event : PluginEvent(true) {
    internal val PLUGIN_DIR_FILE = File("plugins/BotInfo")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadAll()

        logger.info("BotInfo loaded.")
    }

    override fun unload() {
        logger.info("BotInfo unloaded.")
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

    override fun globalCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "bot-info")) return
        BotInfo.onSlashCommandInteraction(event)
    }
}