package tw.xinshou.plugin._example.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,

    @SerialName("example_key")
    val exampleKey: String,
)
