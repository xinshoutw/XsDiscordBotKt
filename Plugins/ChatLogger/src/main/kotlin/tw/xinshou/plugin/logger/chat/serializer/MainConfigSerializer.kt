package tw.xinshou.plugin.logger.chat.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MainConfigSerializer(
    @SerialName("log_all")
    val logAll: Boolean = true,
)
