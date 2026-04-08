package tw.xinshou.discord.plugin.musicplayer

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.ComponentHandler
import tw.xinshou.discord.core.command.componentHandler
import tw.xinshou.discord.core.config.ConfigLoader
import tw.xinshou.discord.core.i18n.Localizer
import tw.xinshou.discord.core.plugin.Plugin
import tw.xinshou.discord.core.plugin.PluginConfig
import tw.xinshou.discord.core.plugin.PluginContext
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.plugin.musicplayer.command.guildCommands
import tw.xinshou.discord.plugin.musicplayer.config.ConfigSerializer
import java.io.File

object Event : ListenerAdapter(), Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")
    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var localizer: Localizer
    internal lateinit var pluginDirectory: File
    internal val logger get() = org.slf4j.LoggerFactory.getLogger(this::class.java)

    override fun PluginContext.onLoad() {
        this@Event.pluginDirectory = pluginDirectory

        pluginConfig = ConfigLoader.load(File(pluginDirectory, "config.yaml"), "/config.yaml")

        if (!pluginConfig.enabled) {
            logger.warn("MusicPlayer is disabled.")
            return
        }

        localizer = Localizer(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.ENGLISH_US,
        )

        pluginConfig.validate()
        logger.info("MusicPlayer plugin loaded successfully")
    }

    override fun PluginContext.onUnload() {
        try {
            logger.info("Cleaning up music managers and audio connections...")
            logger.info("MusicPlayer plugin unloaded successfully")
        } catch (e: Exception) {
            logger.error("Error during plugin unload", e)
        }
    }

    override fun PluginContext.onReload() {
        onLoad()
        if (!pluginConfig.enabled) return
        MusicPlayer.reload()
    }

    override fun commands(): List<CommandHandler> {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands(localizer)
    }

    override fun components(): List<ComponentHandler> {
        val prefix = config.componentPrefix
        if (prefix.isBlank()) return emptyList()

        return listOf(
            componentHandler(prefix) {
                onButton = { event ->
                    try {
                        logger.debug("Processing button interaction: ${event.componentId} from user: ${event.user.name}")
                        MusicPlayer.onButtonInteraction(event)
                    } catch (e: Exception) {
                        logger.error("Error processing button interaction: ${event.componentId}", e)
                        val errorMessage = if (pluginConfig.messages.showDetailedErrors) "Error: ${e.message}" else "Error occurred."
                        if (event.isAcknowledged) event.hook.editOriginal(errorMessage).queue()
                        else event.reply(errorMessage).setEphemeral(true).queue()
                    }
                }
            }
        )
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (!::pluginConfig.isInitialized || !pluginConfig.enabled) return
        try {
            if ((event.name == "play" && event.focusedOption.name == "query") ||
                (event.name == "volume" && event.focusedOption.name == "level")
            ) {
                MusicPlayer.onCommandAutoCompleteInteraction(event)
            } else {
                event.replyChoices(emptyList()).queue()
            }
        } catch (e: Exception) {
            logger.error("Error processing auto-complete interaction", e)
            event.replyChoices(emptyList()).queue()
        }
    }
}
