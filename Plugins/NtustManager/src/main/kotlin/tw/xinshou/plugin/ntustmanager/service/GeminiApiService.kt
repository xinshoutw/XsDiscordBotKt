package tw.xinshou.plugin.ntustmanager.service

import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import tw.xinshou.plugin.ntustmanager.announcement.AnnouncementLink
import tw.xinshou.plugin.ntustmanager.config.ConfigSerializer
import tw.xinshou.plugin.ntustmanager.util.UrlUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Gemini 2.5 Flash 客戶端（google-genai SDK）
 *
 * - 模型：gemini-2.5-flash
 * - 透過官方 SDK 呼叫，不再手刻 HTTP 與 JSON
 * - 保留你原本的 Public API 與重試/鍵輪換語意
 */
class GeminiApiService(private val config: ConfigSerializer) {

    private val logger = LoggerFactory.getLogger(GeminiApiService::class.java)

    // === 常數 ===
    private val modelId = "gemini-2.5-flash"
    private val maxRequestsPerMinute = 10
    private val rateLimitWindowMs = 60_000L // 1 minute in milliseconds

    // === 狀態 ===
    private val apiKeyIndex = AtomicInteger(0)

    // === 速率限制 ===
    // 每個 API Key 的使用時間戳記錄 (key -> list of timestamps)
    private val apiKeyUsageMap = ConcurrentHashMap<String, MutableList<Long>>()
    private val rateLimitLock = ReentrantLock()

    /** 清理過期的使用記錄 */
    private fun cleanupExpiredUsage(apiKey: String, currentTime: Long) {
        val usageList = apiKeyUsageMap[apiKey] ?: return
        usageList.removeAll { timestamp -> currentTime - timestamp > rateLimitWindowMs }
    }

    /** 檢查 API Key 是否可以使用（未超過速率限制） */
    private fun canUseApiKey(apiKey: String): Boolean {
        return rateLimitLock.withLock {
            val currentTime = System.currentTimeMillis()
            cleanupExpiredUsage(apiKey, currentTime)
            val usageList = apiKeyUsageMap.getOrPut(apiKey) { mutableListOf() }
            usageList.size < maxRequestsPerMinute
        }
    }

    /** 記錄 API Key 的使用 */
    private fun recordApiKeyUsage(apiKey: String) {
        rateLimitLock.withLock {
            val currentTime = System.currentTimeMillis()
            val usageList = apiKeyUsageMap.getOrPut(apiKey) { mutableListOf() }
            usageList.add(currentTime)
            cleanupExpiredUsage(apiKey, currentTime)
        }
    }

    /** 尋找可用的 API Key，如果都被限制則返回 null */
    private fun findAvailableKey(): String? {
        // 先嘗試從當前索引開始找可用的 key
        val startIndex = apiKeyIndex.get() % config.apiKeys.size
        for (i in config.apiKeys.indices) {
            val keyIndex = (startIndex + i) % config.apiKeys.size
            val apiKey = config.apiKeys[keyIndex]
            if (canUseApiKey(apiKey)) {
                apiKeyIndex.set(keyIndex + 1) // 更新索引為下一個
                return apiKey
            }
        }
        return null
    }

    /** 計算需要等待多久才能使用任一 API Key */
    private fun calculateWaitTime(): Long {
        return rateLimitLock.withLock {
            val currentTime = System.currentTimeMillis()
            var minWaitTime = Long.MAX_VALUE

            for (apiKey in config.apiKeys) {
                val usageList = apiKeyUsageMap[apiKey] ?: continue
                if (usageList.isEmpty()) {
                    return 0L // 有空閒的 key，不需要等待
                }

                // 找到最舊的使用記錄
                val oldestUsage = usageList.minOrNull() ?: continue
                val waitTime = rateLimitWindowMs - (currentTime - oldestUsage)
                if (waitTime > 0 && waitTime < minWaitTime) {
                    minWaitTime = waitTime
                }
            }

            if (minWaitTime == Long.MAX_VALUE) 0L else minWaitTime
        }
    }

    /** 用指定 API key 建立一個 Client（每次 call/每次重試都以此 key 建新 client） */
    private fun newClient(apiKey: String): Client {
        // SDK 支援直接以 API Key 建 Client（等同從 GOOGLE_API_KEY 讀取，但這裡顯式指定以支援輪換）
        return Client.builder()
            .apiKey(apiKey)
            .build()
    }

    /** 將樣板變數寫入最終 Prompt */
    private fun materializePrompt(html: String, link: AnnouncementLink): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return config.prompt
            .replace("{{page_title}}", link.title)
            .replace("{{page_url}}", link.url)
            .replace("{{base_url}}", "https://${UrlUtils.extractDomainFromBaseUrl(link.url)}")
            .replace("{{now}}", now)
            .replace("{{html}}", html)
    }

    /** 單次呼叫（不含高階重試），回傳文字或 null */
    private fun callOnce(client: Client, prompt: String): String? {
        // 文字內容：單一 Part 即可；若未來要多模態，加更多 Part（inlineData/fileData）
        val content: Content = Content.fromParts(Part.fromText(prompt))

        // 可選的 generation 設定（等價你先前的 maxTokens/temperature 等）
        val genCfg: GenerateContentConfig = GenerateContentConfig.builder().apply {
            temperature(0.1f)
        }.build()

        val resp: GenerateContentResponse = client.models.generateContent(modelId, content, genCfg)
        resp.candidates()
        // 把第一個候選裡的所有 text part 串起來
        val text: String? =
            resp.candidates().orElse(emptyList())
                .firstOrNull()?.content()?.get()
                ?.parts()?.get()?.joinToString(separator = "") { p -> p.text().orElse("") }
                ?.trim()

        return text?.takeIf { it.isNotBlank() }
    }

    /**
     * 對外：處理 HTML（含鍵輪換 + 指數退避重試 + 速率限制）
     */
    suspend fun processHtmlContentWithRetry(
        html: String,
        link: AnnouncementLink,
        maxRetries: Int = 10
    ): String? = withContext(Dispatchers.IO) {
        var attempt = 0
        var last: String? = null

        while (attempt < maxRetries) {
            // 尋找可用的 API Key
            val key = findAvailableKey()

            if (key == null) {
                // 所有 API Key 都被速率限制，計算等待時間
                val waitTime = calculateWaitTime()
                if (waitTime > 0) {
                    logger.info("All API keys rate limited, waiting {} ms", waitTime)
                    delay(waitTime)
                    continue // 等待後重新嘗試，不增加 attempt 計數
                } else {
                    // 理論上不應該發生，但作為安全措施
                    logger.warn("No available API key found but no wait time calculated")
                    delay(1000L) // 等待 1 秒後重試
                    continue
                }
            }

            try {
                val prompt = materializePrompt(html, link)
                val client = newClient(key)

                // 記錄 API Key 使用
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
                    1 -> 60_000L    // 60 seconds = 1min
                    2 -> 300_000L   // 300 seconds = 5min
                    3 -> 900_000L   // 900 seconds = 15min
                    else -> 1800_000L // 1800 seconds = 30min
                }
                delay(backoffMs)
            }
        }

        logger.error("Gemini 2.5 Flash failed after {} attempts", maxRetries)
        last
    }

    /** 設定檢查（與先前相同） */
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

    /** 監控資訊 */
    fun getApiStats(): Map<String, Any> {
        val currentTime = System.currentTimeMillis()
        val keyUsageStats = mutableMapOf<String, Int>()

        rateLimitLock.withLock {
            for (apiKey in config.apiKeys) {
                val usageList = apiKeyUsageMap[apiKey] ?: emptyList()
                // 計算在當前時間窗口內的使用次數
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
