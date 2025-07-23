package tw.xinshou.loader

import com.github.ajalt.clikt.core.parse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.BotLoader
import tw.xinshou.loader.cli.JLineManager
import tw.xinshou.loader.logger.LogBackManager
import tw.xinshou.loader.mongodb.CacheDbServer
import tw.xinshou.loader.util.Arguments


internal fun main(args: Array<String>) = runBlocking {
    val logger = LoggerFactory.getLogger(this::class.java)
    val stopSignal = CompletableDeferred<Unit>()

    try {
        LogBackManager.configureSystem()
        Arguments.parse(args)
        CacheDbServer.start()
        BotLoader.start()
        JLineManager.start(scope = this, stopSignal = stopSignal)
        stopSignal.await()
    } catch (e: Exception) {
        logger.error("An unexpected error occurred in tw.xinshou.loader.main:", e)
    } finally {
        BotLoader.stop()
        CacheDbServer.close()
        JLineManager.stop()
        LogBackManager.uninstall()
        logger.info("Application terminated.")
    }
}