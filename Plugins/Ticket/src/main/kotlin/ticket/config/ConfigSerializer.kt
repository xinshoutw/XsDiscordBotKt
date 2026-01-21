package tw.xinshou.discord.plugin.ticket.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
