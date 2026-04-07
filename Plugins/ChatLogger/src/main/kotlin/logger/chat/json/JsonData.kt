package tw.xinshou.discord.plugin.logger.chat.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DataContainer(
    @SerialName("allow_mode")
    var allowMode: Boolean,

    var allow: MutableSet<Long>,
    var block: MutableSet<Long>,
)

internal typealias JsonDataClass = MutableMap<String, DataContainer>
