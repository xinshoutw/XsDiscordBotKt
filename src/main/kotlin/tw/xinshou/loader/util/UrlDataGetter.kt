package tw.xinshou.loader.util

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.IOException

object UrlDataGetter {
    private val client = OkHttpClient()
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Sends a POST request to the specified URL with a given payload.
     *
     * @param url The URL to which the request is sent.
     * @param payload The payload to be sent with the request.
     * @param contentType The type of content, defaulting to "application/json; charset=UTF-8".
     * @param cookie Optional cookie header.
     * @param authorization Optional authorization header.
     * @return The response body as a string if successful, null otherwise.
     */
    fun postData(
        url: String,
        payload: String,
        contentType: String? = "application/json; charset=UTF-8",
        cookie: String? = null,
        authorization: String? = null
    ): String? {
        return try {
            val requestBody = payload.toRequestBody(contentType?.toMediaTypeOrNull())
            val request = Request.Builder().apply {
                url(url)
                post(requestBody)
                contentType?.let { addHeader("Content-Type", it) }
                authorization?.let { addHeader("Authorization", it) }
                cookie?.let { addHeader("Cookie", it) }
            }.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string()
                else {
                    logger.warn("POST request failed with status: {}", response.code)
                    null
                }
            }
        } catch (e: IOException) {
            logger.error("POST request failed: {}", e.message, e)
            null
        }
    }

    /**
     * Sends a GET request to the specified URL.
     *
     * @param urlStr The URL to which the request is sent.
     * @param authorization Optional authorization header.
     * @return The response body as a string if successful, null otherwise.
     */
    fun getData(urlStr: String, authorization: String? = null): String? {
        return try {
            val request = Request.Builder().apply {
                url(urlStr)
                get()
                addHeader("Content-Type", "application/json; charset=UTF-8")
                authorization?.let { addHeader("Authorization", it) }
            }.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string()
                else {
                    logger.warn("GET request failed with status: {}", response.code)
                    null
                }
            }
        } catch (e: IOException) {
            logger.error("GET request failed: {}", e.message, e)
            null
        }
    }
}
