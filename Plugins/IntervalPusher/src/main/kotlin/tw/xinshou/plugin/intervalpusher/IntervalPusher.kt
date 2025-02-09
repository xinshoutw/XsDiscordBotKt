package tw.xinshou.plugin.intervalpusher

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.BotLoader.jdaBot
import java.io.IOException
import kotlin.coroutines.resumeWithException

internal class IntervalPusher(
    private val originUrl: String,
    private val intervalSeconds: Int,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient()
    private var job: Job? = null

    // Logger 實例
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // 掛起函數來獲取 ping 值並構建 URL
    private suspend fun buildUrl(url: String): String {
        return try {
            val ping = getRestPing()
            url.replace("%ping%", ping.toString())
        } catch (e: Exception) {
            logger.warn("Failed to get ping: {}!", e.message)
            url.replace("%ping%", "-1")
        }
    }

    // 掛起函數來獲取 JDA 的 REST Ping
    private suspend fun getRestPing(): Long = suspendCancellableCoroutine { cont ->
        jdaBot.restPing.queue({ ping ->
            if (cont.isActive) {
                cont.resume(ping) { cause, _, _ ->
                    logger.warn("Coroutine was cancelled while waiting for restPing! Cause: $cause!")
                }
            }
        }, { throwable ->
            if (cont.isActive) {
                cont.resumeWithException(throwable)
            }
        })
    }

    // 開始 IntervalPusher
    fun start() {
        if (job?.isActive == true) {
            logger.warn("IntervalPusher is already running!")
            return
        }

        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // 構建包含最新 ping 的 URL
                    val updatedUrl = buildUrl(originUrl)

                    val request = Request.Builder()
                        .url(updatedUrl)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            when (response.code) {
                                404 -> {
                                    logger.warn("Status Monitor refused the connection! (Code: 404)!")
                                    logger.warn("Breaking the heartbeat loop!")
                                    stop()
                                }

                                521 -> logger.warn("Status Monitor is OFFLINE! (Code: 521)!") // Response from Cloudflare
                                else -> logger.error("Query URL $updatedUrl failed, code: ${response.code}!")
                            }
                        } else {
                            logger.debug("Successfully queried URL: $updatedUrl.")
                        }
                    }
                } catch (e: IOException) {
                    logger.error("Query URL $originUrl internet error: ${e.message}!")
                } catch (e: Exception) {
                    logger.error("Query URL $originUrl failed: ${e.message}!")
                }
                delay(intervalSeconds * 1000L)
            }
        }

        logger.info("IntervalPusher started.")
    }

    // 停止 IntervalPusher
    fun stop() {
        job?.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        client.cache?.close()
        logger.info("IntervalPusher stopped.")
    }
}
