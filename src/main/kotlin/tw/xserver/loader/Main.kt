package tw.xserver.loader

import com.github.ajalt.clikt.core.parse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import tw.xserver.loader.base.BotLoader
import tw.xserver.loader.cli.JLineManager
import tw.xserver.loader.logger.LogBackManager
import tw.xserver.loader.util.Arguments


internal fun main(args: Array<String>) = runBlocking {
    val logger = LoggerFactory.getLogger(this::class.java)
    val stopSignal = CompletableDeferred<Unit>()

    try {
        LogBackManager.configureSystem()
        Arguments.parse(args)
        BotLoader.start()
        JLineManager.start(scope = this, stopSignal = stopSignal)
        stopSignal.await()
    } catch (e: Exception) {
        logger.error("An unexpected error occurred in main:", e)
    } finally {
        BotLoader.stop()
        JLineManager.stop()
        LogBackManager.uninstall()
        logger.info("Application terminated.")
    }
}