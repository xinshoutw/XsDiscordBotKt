package tw.xinshou.plugin.musicplayer

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.localizations.DiscordLocalizationExporter
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.musicplayer.command.guildCommands
import tw.xinshou.plugin.musicplayer.command.lang.CmdFileSerializer
import tw.xinshou.plugin.musicplayer.command.lang.CmdLocalizations
import tw.xinshou.plugin.musicplayer.serializer.MainConfigSerializer
import java.io.File
import java.io.IOException

/**
 * MusicPlayer 插件的主要事件處理器
 *
 * 處理插件的載入、卸載、配置重載和各種 Discord 事件
 * 支援多語言本地化、動態搜索自動完成等功能
 */
object Event : PluginEvent(true) {
    internal val PLUGIN_DIR_FILE = File("plugins/MusicPlayer")
    internal const val COMPONENT_PREFIX = "music_player@"
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal lateinit var config: MainConfigSerializer

    /**
     * 插件載入時的初始化
     */
    override fun load() {
        try {
            fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)

            reload(true)
        } catch (e: Exception) {
            logger.error("Failed to load MusicPlayer plugin", e)
            throw e
        }
    }

    /**
     * 插件卸載時的清理工作
     */
    override fun unload() {
        try {
            // 清理音樂管理器和連接
            logger.info("Cleaning up music managers and audio connections...")
            logger.info("MusicPlayer plugin unloaded successfully")
        } catch (e: Exception) {
            logger.error("Error during plugin unload", e)
        }
    }

    override fun reload(init: Boolean) {
        try {
            fileGetter.readInputStream("config.yaml").use { inputStream ->
                config = Yaml().decodeFromStream<MainConfigSerializer>(inputStream)
            }

            if (!::config.isInitialized) {
                logger.warn("Configuration not found")
                throw IllegalStateException("Configuration not found")
            }

            if (!config.enabled) {
                logger.warn("Plugin is disabled in configuration. Enable it by setting 'enabled: true' in config.yaml")
                return
            }
            config.validate()
            logger.info("Configuration file reloaded successfully")

            fileGetter.exportDefaultDirectory("lang")
            DiscordLocalizationExporter(
                pluginDirFile = PLUGIN_DIR_FILE,
                fileName = "register.yaml",
                defaultLocale = DiscordLocale.ENGLISH_US,
                clazzSerializer = CmdFileSerializer::class,
                clazzLocalization = CmdLocalizations::class
            )
            logger.info("Language files reloaded successfully")

        } catch (e: IOException) {
            logger.error("Failed to load configuration file: ${PLUGIN_DIR_FILE.canonicalPath}/config.yaml", e)
        } catch (e: IllegalStateException) {
            logger.error("Configuration validation failed: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Fail to reload", e)
        }
    }

    /**
     * 取得公會指令配置
     */
    override fun guildCommands(): Array<CommandData> = guildCommands

    /**
     * 處理斜線指令互動事件
     */
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        try {
            logger.debug("Processing slash command: ${event.name} from user: ${event.user.name}")
            MusicPlayer.onSlashCommandInteraction(event)
        } catch (e: Exception) {
            logger.error("Error processing slash command: ${event.name}", e)

            // 如果配置允許顯示詳細錯誤
            val errorMessage = if (::config.isInitialized && config.messages.showDetailedErrors) {
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

    /**
     * 處理按鈕互動事件
     */
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)) return

        try {
            logger.debug("Processing button interaction: ${event.componentId} from user: ${event.user.name}")
            MusicPlayer.onButtonInteraction(event)
        } catch (e: Exception) {
            logger.error("Error processing button interaction: ${event.componentId}", e)

            val errorMessage = if (::config.isInitialized && config.messages.showDetailedErrors) {
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

    /**
     * 處理指令自動完成事件
     *
     * 為 play 指令提供動態的 YouTube 搜索建議
     * 為 volume 指令提供當前音量建議
     * 注意：這個方法需要在主事件監聽器中註冊
     */

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