package tw.xinshou.plugin.dynamicvoicechannel.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
