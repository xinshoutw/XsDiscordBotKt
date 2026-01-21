package tw.xinshou.discord.plugin.ntustcourse.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
