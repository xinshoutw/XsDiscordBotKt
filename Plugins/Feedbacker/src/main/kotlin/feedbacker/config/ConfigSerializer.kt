package tw.xinshou.discord.plugin.feedbacker.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,

    @SerialName("form_not_you")
    val formNotYou: String,

    @SerialName("form_no_permission")
    val formNoPermission: String,

    @SerialName("form_success")
    val formSuccess: String,
)
