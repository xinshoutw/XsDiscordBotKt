package tw.xinshou.plugin.logger.chat.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaceholderSerializer(
    val empty: String,

    @SerialName("allow_list_format")
    val allowListFormat: String,

    @SerialName("block_list_format")
    val blockListFormat: String,
)