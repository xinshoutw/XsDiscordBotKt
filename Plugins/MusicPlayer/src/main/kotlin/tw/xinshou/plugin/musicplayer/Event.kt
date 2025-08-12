package tw.xinshou.plugin.musicplayer

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.loader.localizations.StringLocalizer
import tw.xinshou.loader.plugin.PluginEventConfigure
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.musicplayer.command.CmdFileSerializer
import tw.xinshou.plugin.musicplayer.command.commandStringSet
import tw.xinshou.plugin.musicplayer.command.guildCommands
import tw.xinshou.plugin.musicplayer.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.ENGLISH_US,
            clazzSerializer = CmdFileSerializer::class,
        )

        if (!config.enabled) {
            logger.warn("Plugin is disabled in configuration. Enable it by setting 'enabled: true' in config.yaml")
            return
        }

        config.validate()
        logger.info("MusicPlayer plugin loaded successfully")
    }

    override fun unload() {
        try {
            logger.info("Cleaning up music managers and audio connections...")
            logger.info("MusicPlayer plugin unloaded successfully")
        } catch (e: Exception) {
            logger.error("Error during plugin unload", e)
        }
    }

    override fun reload() {
        super.reload()

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.ENGLISH_US,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return

        try {
            logger.debug("Processing slash command: ${event.name} from user: ${event.user.name}")
            MusicPlayer.onSlashCommandInteraction(event)
        } catch (e: Exception) {
            logger.error("Error processing slash command: ${event.name}", e)

            val errorMessage = if (config.messages.showDetailedErrors) {
                "❌ 處理指令時發生錯誤：${e.message}"
            } else {
                "❌ 處理指令時發生錯誤，請稍後再試"
            }

            if (event.isAcknowledged) {
                event.hook.editOriginal(errorMessage).queue()
            } else {
                event.reply(errorMessage).setEphemeral(true).queue()
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return

        try {
            logger.debug("Processing button interaction: ${event.componentId} from user: ${event.user.name}")
            MusicPlayer.onButtonInteraction(event)
        } catch (e: Exception) {
            logger.error("Error processing button interaction: ${event.componentId}", e)

            val errorMessage = if (config.messages.showDetailedErrors) {
                "❌ 處理按鈕互動時發生錯誤：${e.message}"
            } else {
                "❌ 處理按鈕互動時發生錯誤，請稍後再試"
            }

            if (event.isAcknowledged) {
                event.hook.editOriginal(errorMessage).queue()
            } else {
                event.reply(errorMessage).setEphemeral(true).queue()
            }
        }
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        try {
            logger.debug("Processing auto-complete for command: ${event.name}, option: ${event.focusedOption.name}")

            // 檢查是否為支援的指令和選項
            if ((event.name == "play" && event.focusedOption.name == "query") ||
                (event.name == "volume" && event.focusedOption.name == "level")
            ) {
                MusicPlayer.onCommandAutoCompleteInteraction(event)
            } else {
                // 對於不支援的自動完成請求，返回空選項
                event.replyChoices(emptyList()).queue()
            }

        } catch (e: Exception) {
            logger.error("Error processing auto-complete interaction", e)
            // 自動完成失敗時返回空選項，避免用戶看到錯誤
            event.replyChoices(emptyList()).queue()
        }
    }
}