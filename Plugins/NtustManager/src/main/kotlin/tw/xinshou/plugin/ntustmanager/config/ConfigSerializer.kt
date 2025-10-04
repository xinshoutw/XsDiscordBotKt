package tw.xinshou.plugin.ntustmanager.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean,
    @SerialName("api-keys")
    val apiKeys: List<String>,
    val prompt: String,
    @SerialName("fetch-interval")
    val fetchInterval: Long = 3600,
) {
    init {
        require(apiKeys.isNotEmpty()) { "apiKeys must not be empty" }
        require(apiKeys.none { it == "sk-thisisatestkey1234567890abcdef1234567890" }) {
            "You must replace the test API key with your actual API key!"
        }

        require(prompt.isNotBlank()) { "prompt must not be blank" }
        require(fetchInterval >= 60) { "fetchInterval must be at least 60 seconds" }
    }
}
