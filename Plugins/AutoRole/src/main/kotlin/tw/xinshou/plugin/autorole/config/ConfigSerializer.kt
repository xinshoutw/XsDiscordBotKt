package tw.xinshou.plugin.autorole.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
    val guilds: List<GuildConfigSerializer>,
) {
    @Serializable
    data class GuildConfigSerializer(
        @SerialName("guild_id")
        val guildId: Long,

        @SerialName("role_ids")
        val roleIds: List<Long>,
    ) {
        init {
            require(guildId > 0) { "guildId must be greater than 0" }
            require(roleIds.isNotEmpty()) { "roleIds must not be empty" }
        }
    }
}
