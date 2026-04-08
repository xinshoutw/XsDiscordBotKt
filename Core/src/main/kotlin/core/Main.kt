package tw.xinshou.discord.core

import tw.xinshou.discord.core.cli.ConsoleManager
import tw.xinshou.discord.core.config.BotConfig
import tw.xinshou.discord.core.dashboard.DashboardServer
import tw.xinshou.discord.core.database.DatabaseProvider
import tw.xinshou.discord.core.di.coreModule
import tw.xinshou.discord.core.logger.LogbackConfig
import tw.xinshou.discord.core.plugin.PluginRegistry
import tw.xinshou.discord.core.util.Arguments
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

    // Allow CLI token override, then validate
    val effectiveConfig = if (Arguments.token != null) {
        config.copy(botToken = Arguments.token!!)
    } else config
    require(effectiveConfig.botToken != "YOUR_BOT_TOKEN_HERE") {
        "Bot token is not set. Provide it in config.yaml or via -t/--token"
    }

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
