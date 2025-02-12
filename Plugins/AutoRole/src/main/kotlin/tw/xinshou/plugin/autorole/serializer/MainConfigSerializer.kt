package tw.xinshou.plugin.autorole.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MainConfigSerializer(
    val guilds: List<GuildConfigSerializer>,
) {
    @Serializable
    data class GuildConfigSerializer(
        @SerialName("guild_id")
        val guildId: Long,

        @SerialName("role_ids")
        val roleIds: List<Long>,
    )
}
