package tw.xinshou.discord.plugin.dynamicvoicechannel.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
