package tw.xinshou.loader.base

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.SettingsLoader.token
import tw.xinshou.loader.builtin.statuschanger.StatusChanger
import tw.xinshou.loader.logger.InteractionLogger
import tw.xinshou.loader.plugin.yaml.processMemberCachePolicy
import tw.xinshou.loader.util.Arguments
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * Main loader for the bot application, handles bot initialization, and management of events and plugins.
 */
object BotLoader {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val ROOT_PATH: String = Paths.get("").toAbsolutePath().toString()
    lateinit var jdaBot: JDA
        private set
    lateinit var bot: User
        private set

    /**
     * Starts the bot by loading settings, initializing plugins, and setting up JDA.
     */
    internal fun start() {
        if (UpdateChecker.versionCheck()) {
            logger.error("Version check failed, exiting.")
            exitProcess(2)
        }

        SettingsLoader.run()
        if (Arguments.noBuild) {
            logger.warn("Skip building bot!")
            return
        }

        PluginLoader.preLoad()
        jdaBot = JDABuilder.createDefault(Arguments.botToken ?: token)
            .setBulkDeleteSplittingEnabled(false)
            .disableCache(
                CacheFlag.ACTIVITY,
                CacheFlag.VOICE_STATE,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ONLINE_STATUS,
            )
            .setEnabledIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.SCHEDULED_EVENTS,
                GatewayIntent.GUILD_EXPRESSIONS,
            )
            .setMemberCachePolicy(processMemberCachePolicy(PluginLoader.memberCachePolicies))
            .enableCache(PluginLoader.cacheFlags)
            .enableIntents(PluginLoader.intents)
//            .setAudioSendFactory(NativeAudioSendFactory())
            .build()
            .awaitReady()

        bot = jdaBot.selfUser

        jdaBot.apply {
            // Register Builtin Tool
            addEventListener(InteractionLogger)

            // Register Plugins' Event Listener
            addEventListener(ListenerManager(PluginLoader.guildCommands))

            // Register Plugins' Event Listener
            PluginLoader.listenersQueue.forEach { plugin -> addEventListener(plugin) }

            // Register Plugins' Global Commands
            updateCommands().addCommands(PluginLoader.globalCommands).queue()
        }

        PluginLoader.run()
        logger.info("Bot initialized.")
    }

    /**
     * Reloads plugins and settings, resets status changer.
     */
    internal fun reload() {
        try {
            PluginLoader.reload()
            SettingsLoader.run()
            StatusChanger.run()
            logger.info("Application reloaded successfully.")
        } catch (e: Exception) {
            logger.error("Failed to reload application:", e)
        }
    }

    /**
     * Stops the bot and cleans up resources, including shutting down JDA and unloading plugins.
     */
    internal fun stop() {
        try {
            if (::jdaBot.isInitialized) {
                jdaBot.apply {
                    registeredListeners.forEach { removeEventListener(it) }
                    shutdown()
                    awaitShutdown()
                }
            }

            PluginLoader.apply {
                pluginQueue.reversed().forEach { (name, plugin) ->
                    plugin.unload()
                    logger.info("{} unloaded successfully.", name)
                }
            }

            StatusChanger.stop()
            logger.info("Bot shutdown completed.")
        } catch (e: Exception) {
            logger.error("Failed to stop BotLoader:", e)
        }
    }
}
