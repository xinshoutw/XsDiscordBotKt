package tw.xinshou.plugin.api.google.sheet.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthConfigSerializer(
    @SerialName("client_id")
    val clientId: String,

    @SerialName("client_secret")
    val clientSecret: String,
    val port: Int
)
