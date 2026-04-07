package core

import core.cli.ConsoleManager
import core.config.BotConfig
import core.dashboard.DashboardServer
import core.database.DatabaseProvider
import core.di.coreModule
import core.logger.LogbackConfig
import core.plugin.PluginRegistry
import core.util.Arguments
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.getKoin
import org.slf4j.LoggerFactory

internal fun main(args: Array<String>) = runBlocking {
    LogbackConfig.configureSystem()
    Arguments.parse(args)
    LogbackConfig.setLevel(Arguments.logLevel)

    val logger = LoggerFactory.getLogger("Core")
    val stopSignal = CompletableDeferred<Unit>()

    startKoin {
        modules(coreModule)
    }

    // Resolve from Koin
    val config: BotConfig = getKoin().get()
    val db: DatabaseProvider = getKoin().get()
    val pluginRegistry: PluginRegistry = getKoin().get()
    val dashboard: DashboardServer = getKoin().get()

    // Validate token
    require(config.botToken != "YOUR_BOT_TOKEN_HERE") { "Bot token is not set in config.yaml" }
    // Allow CLI token override
    val effectiveConfig = if (Arguments.token != null) {
        config.copy(botToken = Arguments.token!!)
    } else config

    val bot = BotApplication(
        effectiveConfig,
        pluginRegistry,
        getKoin().get(), // CommandRegistry
        getKoin().get(), // ComponentRegistry
        getKoin().get(), // InteractionLogger
    )

    val console = ConsoleManager(
        onReload = { bot.reload() },
        onStop = { bot.stop() },
    )

    try {
        db.start()
        dashboard.start()
        bot.start()
        logger.info("Bot is ready! (v{})", tw.xinshou.discord.common.Version.VERSION)
        console.start(scope = this, stopSignal = stopSignal)
        stopSignal.await()
    } catch (e: Exception) {
        logger.error("Fatal error", e)
    } finally {
        console.stop()
        bot.stop()
        dashboard.stop()
        db.stop()
        stopKoin()
        LogbackConfig.uninstall()
    }
}
