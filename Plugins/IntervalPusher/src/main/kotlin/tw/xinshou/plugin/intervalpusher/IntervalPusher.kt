package tw.xinshou.plugin.intervalpusher

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.BotLoader.jdaBot
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.coroutines.resumeWithException

internal class IntervalPusher(
    private val originUrl: String,
    private val intervalSeconds: Int,
    private val scope: CoroutineScope
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
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

                    // 使用 Java HttpClient 建立請求
                    val request = HttpRequest.newBuilder()
                        .uri(URI.create(updatedUrl))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build()

                    // 使用 suspendCancellableCoroutine 將 CompletableFuture 轉換為 suspend function
                    val response = suspendCancellableCoroutine<HttpResponse<String>> { cont ->
                        val future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())

                        future.whenComplete { result, throwable ->
                            if (throwable != null) {
                                if (cont.isActive) {
                                    cont.resumeWithException(throwable)
                                }
                            } else {
                                if (cont.isActive) {
                                    cont.resume(result) { cause, _, _ ->
                                        logger.warn("Coroutine was cancelled while waiting for HTTP response! Cause: $cause!")
                                    }
                                }
                            }
                        }

                        // 當協程被取消時，取消 HTTP 請求
                        cont.invokeOnCancellation {
                            future.cancel(true)
                        }
                    }

                    // 處理 HTTP 回應
                    when (response.statusCode()) {
                        in 200..299 -> {
                            logger.debug("Successfully queried URL: $updatedUrl.")
                        }

                        404 -> {
                            logger.warn("Status Monitor refused the connection! (Code: 404)!")
                            logger.warn("Breaking the heartbeat loop!")
                            stop()
                        }

                        521 -> {
                            logger.warn("Status Monitor is OFFLINE! (Code: 521)!") // Response from Cloudflare
                        }

                        else -> {
                            logger.error("Query URL $updatedUrl failed, code: ${response.statusCode()}!")
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
        // Java HttpClient 會自動管理資源，不需要手動清理
        logger.info("IntervalPusher stopped.")
    }
}
