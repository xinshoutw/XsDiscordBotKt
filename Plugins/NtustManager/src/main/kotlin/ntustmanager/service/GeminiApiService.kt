package ntustmanager.service

import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import ntustmanager.announcement.AnnouncementLink
import ntustmanager.config.ConfigSerializer
import ntustmanager.util.UrlUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class GeminiApiService(private val config: ConfigSerializer) {

    private val logger = LoggerFactory.getLogger(GeminiApiService::class.java)

    private val modelId = "gemini-2.5-flash"
    private val maxRequestsPerMinute = 10
    private val rateLimitWindowMs = 60_000L

    private val apiKeyIndex = AtomicInteger(0)

    private val apiKeyUsageMap = ConcurrentHashMap<String, MutableList<Long>>()
    private val rateLimitLock = ReentrantLock()

    private fun cleanupExpiredUsage(apiKey: String, currentTime: Long) {
        val usageList = apiKeyUsageMap[apiKey] ?: return
        usageList.removeAll { timestamp -> currentTime - timestamp > rateLimitWindowMs }
    }

    private fun canUseApiKey(apiKey: String): Boolean {
        return rateLimitLock.withLock {
            val currentTime = System.currentTimeMillis()
            cleanupExpiredUsage(apiKey, currentTime)
            val usageList = apiKeyUsageMap.getOrPut(apiKey) { mutableListOf() }
            usageList.size < maxRequestsPerMinute
        }
    }

    private fun recordApiKeyUsage(apiKey: String) {
        rateLimitLock.withLock {
            val currentTime = System.currentTimeMillis()
            val usageList = apiKeyUsageMap.getOrPut(apiKey) { mutableListOf() }
            usageList.add(currentTime)
            cleanupExpiredUsage(apiKey, currentTime)
        }
    }

    private fun findAvailableKey(): String? {
        val startIndex = apiKeyIndex.get() % config.apiKeys.size
        for (i in config.apiKeys.indices) {
            val keyIndex = (startIndex + i) % config.apiKeys.size
            val apiKey = config.apiKeys[keyIndex]
            if (canUseApiKey(apiKey)) {
                apiKeyIndex.set(keyIndex + 1)
                return apiKey
            }
        }
        return null
    }

    private fun calculateWaitTime(): Long {
        return rateLimitLock.withLock {
            val currentTime = System.currentTimeMillis()
            var minWaitTime = Long.MAX_VALUE

            for (apiKey in config.apiKeys) {
                val usageList = apiKeyUsageMap[apiKey] ?: continue
                if (usageList.isEmpty()) {
                    return 0L
                }

                val oldestUsage = usageList.minOrNull() ?: continue
                val waitTime = rateLimitWindowMs - (currentTime - oldestUsage)
                if (waitTime > 0 && waitTime < minWaitTime) {
                    minWaitTime = waitTime
                }
            }

            if (minWaitTime == Long.MAX_VALUE) 0L else minWaitTime
        }
    }

    private fun newClient(apiKey: String): Client {
        return Client.builder()
            .apiKey(apiKey)
            .build()
    }

    private fun materializePrompt(html: String, link: AnnouncementLink): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return config.prompt
            .replace("{{page_title}}", link.title)
            .replace("{{page_url}}", link.url)
            .replace("{{base_url}}", "https://${UrlUtils.extractDomainFromBaseUrl(link.url)}")
            .replace("{{now}}", now)
            .replace("{{html}}", html)
    }

    private fun callOnce(client: Client, prompt: String): String? {
        val content: Content = Content.fromParts(Part.fromText(prompt))

        val genCfg: GenerateContentConfig = GenerateContentConfig.builder().apply {
            temperature(0.1f)
        }.build()

        val resp: GenerateContentResponse = client.models.generateContent(modelId, content, genCfg)
        resp.candidates()
        val text: String? =
            resp.candidates().orElse(emptyList())
                .firstOrNull()?.content()?.get()
                ?.parts()?.get()?.joinToString(separator = "") { p -> p.text().orElse("") }
                ?.trim()

        return text?.takeIf { it.isNotBlank() }
    }

    suspend fun processHtmlContentWithRetry(
        html: String,
        link: AnnouncementLink,
        maxRetries: Int = 10
    ): String? = withContext(Dispatchers.IO) {
        var attempt = 0
        var last: String? = null

        while (attempt < maxRetries) {
            val key = findAvailableKey()

            if (key == null) {
                val waitTime = calculateWaitTime()
                if (waitTime > 0) {
                    logger.info("All API keys rate limited, waiting {} ms", waitTime)
                    delay(waitTime)
                    continue
                } else {
                    logger.warn("No available API key found but no wait time calculated")
                    delay(1000L)
                    continue
                }
            }

            try {
                val prompt = materializePrompt(html, link)
                val client = newClient(key)

                recordApiKeyUsage(key)

                last = callOnce(client, prompt)
                if (last != null) {
                    logger.info(
                        "Gemini 2.5 Flash success on attempt {} with key ending in {}",
                        attempt + 1, key.takeLast(4)
                    )
                    return@withContext last
                }
            } catch (e: Exception) {
                logger.warn(
                    "Attempt {} failed with key ending in {}: {}",
                    attempt + 1, key.takeLast(4), e.message
                )
            }

            attempt += 1
            if (attempt < maxRetries) {
                val backoffMs = when (attempt) {
                    1 -> 60_000L
                    2 -> 300_000L
                    3 -> 900_000L
                    else -> 1800_000L
                }
                delay(backoffMs)
            }
        }

        logger.error("Gemini 2.5 Flash failed after {} attempts", maxRetries)
        last
    }

    fun validateConfiguration(): Boolean {
        if (config.apiKeys.isEmpty() || config.apiKeys.any { it.isBlank() }) {
            logger.error("API keys invalid (empty/blank)")
            return false
        }
        if (config.prompt.isBlank()) {
            logger.error("Prompt is blank")
            return false
        }
        val required = listOf("{{page_url}}", "{{base_url}}", "{{now}}", "{{html}}")
        val missing = required.filterNot { config.prompt.contains(it) }
        if (missing.isNotEmpty()) {
            logger.error("Prompt missing variables: {}", missing)
            return false
        }
        logger.info("Gemini 2.5 Flash configuration ok (google-genai)")
        return true
    }

    fun getApiStats(): Map<String, Any> {
        val currentTime = System.currentTimeMillis()
        val keyUsageStats = mutableMapOf<String, Int>()

        rateLimitLock.withLock {
            for (apiKey in config.apiKeys) {
                val usageList = apiKeyUsageMap[apiKey] ?: emptyList()
                val recentUsage = usageList.count { timestamp ->
                    currentTime - timestamp <= rateLimitWindowMs
                }
                keyUsageStats[apiKey.takeLast(4)] = recentUsage
            }
        }

        return mapOf(
            "totalApiKeys" to config.apiKeys.size,
            "currentApiKeyIndex" to (apiKeyIndex.get() % config.apiKeys.size),
            "totalRequests" to apiKeyIndex.get(),
            "promptLength" to config.prompt.length,
            "modelUsed" to modelId,
            "sdk" to "com.google.genai:google-genai",
            "rateLimitConfig" to mapOf(
                "maxRequestsPerMinute" to maxRequestsPerMinute,
                "rateLimitWindowMs" to rateLimitWindowMs
            ),
            "keyUsageInCurrentWindow" to keyUsageStats,
            "availableKeys" to config.apiKeys.count { canUseApiKey(it) }
        )
    }
}
