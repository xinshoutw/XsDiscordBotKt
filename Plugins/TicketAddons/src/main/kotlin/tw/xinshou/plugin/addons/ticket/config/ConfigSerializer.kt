package tw.xinshou.plugin.addons.ticket.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,

    @SerialName("guild_id")
    val guildId: String,

    @SerialName("user_id")
    val userId: String,
    val prefix: List<String>,

    @SerialName("delay_millis")
    val delayMillis: Long,
) {
    init {
        require(guildId.isNotBlank()) { "guildId must not be blank" }
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(prefix.isNotEmpty()) { "prefix must not be empty" }
        require(delayMillis > 0) { "delayMillis must be greater than 0" }
    }
}
