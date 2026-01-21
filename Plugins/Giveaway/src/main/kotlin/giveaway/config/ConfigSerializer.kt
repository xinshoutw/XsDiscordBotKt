package tw.xinshou.discord.plugin.giveaway.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
