package tw.xinshou.discord.core.base

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.SettingsLoader.token
import tw.xinshou.discord.core.builtin.consolelogger.ConsoleLogger
import tw.xinshou.discord.core.builtin.statuschanger.StatusChanger
import tw.xinshou.discord.core.logger.InteractionLogger
import tw.xinshou.discord.core.plugin.yaml.processMemberCachePolicy
import tw.xinshou.discord.core.util.Arguments
import java.nio.file.Paths

/**
 * Main loader for the bot application, handles bot initialization, and management of events and plugins.
 */
object BotLoader {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val lifecycleLock = Any()
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
            return
        }

        SettingsLoader.run()
        if (Arguments.noBuild) {
            logger.warn("Skip building bot!")
            return
        }

        val token = Arguments.botToken ?: token
        if (token == "ODgAAAAAAAAAAADkx.GBBBBI.V6CCCCCCCCCCCCCCCCCCCCCCCC9mfsU") {
            logger.error("Bot token is not set, exiting.")
            throw IllegalStateException("Bot token is not set.")
        }

        PluginLoader.preLoad()
        try {
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
                .build()
                .awaitReady()
        } catch (e: Exception) {
            logger.error("Failed to start bot:", e)
            throw e
        }

        bot = jdaBot.selfUser

        PluginLoader.run()

        jdaBot.apply {
            // Register Builtin Tool
            addEventListener(InteractionLogger)

            // printout PluginLoader.guildCommands for debug
            if (PluginLoader.guildCommands.isNotEmpty()) {
                logger.debug("Guild Commands: {}", PluginLoader.guildCommands.joinToString(", ") { it.name })
            }

            // Register Plugins' Event Listener
            addEventListener(ListenerManager(PluginLoader.guildCommands))

            // Register Plugins' Event Listener
            PluginLoader.listenersQueue.forEach { plugin -> addEventListener(plugin) }

            // Register Plugins' Global Commands
            updateCommands().addCommands(PluginLoader.globalCommands).queue()
        }

        logger.info("Bot initialized.")
    }

    /**
     * Reloads plugins and settings, resets status changer.
     */
    internal fun reload() {
        try {
            reloadAllStrict()
            logger.info("Application reloaded successfully.")
        } catch (e: Exception) {
            logger.error("Failed to reload application:", e)
        }
    }

    internal fun reloadAllStrict() {
        synchronized(lifecycleLock) {
            PluginLoader.reload()
            reloadCoreSettingsStrict()
            refreshCommandsStrict()
        }
    }

    internal fun reloadPluginStrict(pluginName: String) {
        synchronized(lifecycleLock) {
            PluginLoader.reloadPlugin(pluginName)
            refreshCommandsStrict()
        }
    }

    internal fun reloadCoreSettingsStrict() {
        synchronized(lifecycleLock) {
            SettingsLoader.run()
            if (::jdaBot.isInitialized) {
                StatusChanger.run()
                ConsoleLogger.reload()
            }
        }
    }

    internal fun refreshCommandsStrict() {
        synchronized(lifecycleLock) {
            if (!::jdaBot.isInitialized) return

            jdaBot.updateCommands().addCommands(PluginLoader.globalCommands).complete()
            jdaBot.guilds.forEach { guild: Guild ->
                guild.updateCommands().addCommands(PluginLoader.guildCommands).complete()
            }
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
                closeClassLoaders()
            }

            StatusChanger.stop()
            ConsoleLogger.stop()
            logger.info("Bot shutdown completed.")
        } catch (e: Exception) {
            logger.error("Failed to stop BotLoader:", e)
        }
    }
}
