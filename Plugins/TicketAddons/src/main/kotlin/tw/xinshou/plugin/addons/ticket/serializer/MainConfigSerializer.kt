package tw.xinshou.plugin.addons.ticket.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MainConfigSerializer(
    @SerialName("guild_id")
    val guildId: String,

    @SerialName("user_id")
    val userId: String,
    val prefix: List<String>,

    @SerialName("delay_millis")
    val delayMillis: Long,
)
