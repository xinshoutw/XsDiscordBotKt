package tw.xserver.loader.base

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.builtin.StatusChanger
import tw.xserver.loader.logger.InteractionLogger
import tw.xserver.loader.plugin.Event
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess

/**
 * Main loader for the bot application, handles bot initialization, and management of events and plugins.
 */
object MainLoader {
    private val logger: Logger = LoggerFactory.getLogger(MainLoader::class.java)
    val ROOT_PATH: String = System.getProperty("user.dir")
    val guildCommands: ArrayList<CommandData> = ArrayList()
    val globalCommands: ArrayList<CommandData> = ArrayList()
    val listeners: Queue<Event> = ArrayDeque()
    lateinit var jdaBot: JDA
    lateinit var bot: User
    var botId: Long by Delegates.notNull()

    /**
     * Starts the bot by loading settings, initializing plugins, and setting up JDA.
     */
    fun start() {
        if (UpdateChecker.versionCheck()) {
            logger.error("Version check failed, exiting.")
            exitProcess(2)
        }

        SettingsLoader.run()
        PluginLoader.run()

        jdaBot = JDABuilder.createDefault(SettingsLoader.token)
            .setBulkDeleteSplittingEnabled(false)
            .setLargeThreshold(250)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .enableCache(
                CacheFlag.ONLINE_STATUS,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ACTIVITY
            )
            .enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.MESSAGE_CONTENT
            ).build().apply {
                selfUser.also { bot = it }
                botId = bot.idLong

                addEventListener(ListenerManager(guildCommands))
                addEventListener(InteractionLogger())
                listeners.forEach { addEventListener(it) }
                updateCommands().addCommands(globalCommands).queue()
            }

        StatusChanger.run()
        logger.info("Bot initialized and ready.")
    }

    /**
     * Reloads plugins and settings, resets status changer.
     */
    fun reload() {
        PluginLoader.reload()
        SettingsLoader.run()
        StatusChanger.run()
    }

    /**
     * Stops the bot and cleans up resources, including shutting down JDA and unloading plugins.
     */
    fun stop() {
        jdaBot.apply {
            registeredListeners.forEach { removeEventListener(it) }
            shutdown()
        }
        PluginLoader.apply {
            pluginQueue.values.reversed().forEach { it.unload() }
            plugins.clear()
            pluginQueue.clear()
        }

        StatusChanger.stop()
        logger.info("Bot shutdown completed.")
    }
}
