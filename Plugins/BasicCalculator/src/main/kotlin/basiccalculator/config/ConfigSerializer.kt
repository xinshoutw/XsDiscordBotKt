package tw.xinshou.discord.plugin.basiccalculator.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
