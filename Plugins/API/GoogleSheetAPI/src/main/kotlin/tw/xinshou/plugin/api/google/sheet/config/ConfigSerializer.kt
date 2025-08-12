package tw.xinshou.plugin.api.google.sheet.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    @SerialName("client_id")
    val clientId: String,

    @SerialName("client_secret")
    val clientSecret: String,
    val port: Int
) {
    init {
        require(clientId.isNotBlank()) { "clientId must not be blank" }
        require(clientSecret.isNotBlank()) { "clientSecret must not be blank" }
        require(port == -1 || port > 0) { "port must be -1 or > 0" }
    }
}
