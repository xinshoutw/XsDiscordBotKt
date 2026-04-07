package intervalpusher

import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import org.koin.java.KoinJavaComponent.getKoin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private suspend fun buildUrl(url: String): String {
        return try {
            val ping = getRestPing()
            url.replace("%ping%", ping.toString())
        } catch (e: Exception) {
            logger.warn("Failed to get ping: {}!", e.message)
            url.replace("%ping%", "-1")
        }
    }

    private suspend fun getRestPing(): Long {
        val jda: JDA = getKoin().get()
        return suspendCancellableCoroutine { cont ->
            jda.restPing.queue({ ping ->
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
    }

    fun start() {
        if (job?.isActive == true) {
            logger.warn("IntervalPusher is already running!")
            return
        }

        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val updatedUrl = buildUrl(originUrl)

                    val request = HttpRequest.newBuilder()
                        .uri(URI.create(updatedUrl))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build()

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

                        cont.invokeOnCancellation {
                            future.cancel(true)
                        }
                    }

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
                            logger.warn("Status Monitor is OFFLINE! (Code: 521)!")
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

    fun stop() {
        job?.cancel()
        logger.info("IntervalPusher stopped.")
    }
}
