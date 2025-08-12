package tw.xinshou.plugin.logger.chat.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    @SerialName("log_all")
    val logAll: Boolean = true,
)
