package tw.xserver.plugin.logger.chat.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MainConfigSerializer(
    @SerialName("log_all")
    val logAll: Boolean = true,
)
