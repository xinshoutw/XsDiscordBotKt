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
import java.util.concurrent.atomic.AtomicInteger

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

    // === 狀態 ===
    private val apiKeyIndex = AtomicInteger(0)

    /** 依序輪換下一把 API Key（沿用你原本設計） */
    private fun nextKey(): String {
        val idx = apiKeyIndex.getAndIncrement() % config.apiKeys.size
        return config.apiKeys[idx]
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
     * 對外：處理 HTML（含鍵輪換 + 指數退避重試）
     */
    suspend fun processHtmlContentWithRetry(
        html: String,
        link: AnnouncementLink,
        maxRetries: Int = 10
    ): String? = withContext(Dispatchers.IO) {
        var attempt = 0
        var last: String? = null

        while (attempt < maxRetries) {
            val key = nextKey()
            try {
                val prompt = materializePrompt(html, link)
                val client = newClient(key)
                last = callOnce(client, prompt)
                if (last != null) {
                    logger.info("Gemini 2.5 Flash success on attempt {}", attempt + 1)
                    return@withContext last
                }
            } catch (e: Exception) {
                logger.warn("Attempt {} failed with key index {}: {}", attempt + 1, apiKeyIndex.get() - 1, e.message)
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
    fun getApiStats(): Map<String, Any> = mapOf(
        "totalApiKeys" to config.apiKeys.size,
        "currentApiKeyIndex" to (apiKeyIndex.get() % config.apiKeys.size),
        "totalRequests" to apiKeyIndex.get(),
        "promptLength" to config.prompt.length,
        "modelUsed" to modelId,
        "sdk" to "com.google.genai:google-genai"
    )
}
