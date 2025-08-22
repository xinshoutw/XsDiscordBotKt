package tw.xinshou.core

import com.github.ajalt.clikt.core.parse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

import tw.xinshou.core.base.BotLoader
import tw.xinshou.core.cli.JLineManager
import tw.xinshou.core.logger.LogBackManager
import tw.xinshou.core.mongodb.CacheDbServer
import tw.xinshou.core.util.Arguments


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
        logger.error("An unexpected error occurred in tw.xinshou.core.main:", e)
    } finally {
        CacheDbServer.close()
        BotLoader.stop()
        JLineManager.stop()
        LogBackManager.uninstall()
        logger.info("Application terminated.")
    }
}