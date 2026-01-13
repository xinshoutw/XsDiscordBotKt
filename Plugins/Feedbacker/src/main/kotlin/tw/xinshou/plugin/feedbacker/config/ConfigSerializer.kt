package tw.xinshou.plugin.feedbacker.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
    val language: String = "zh-TW",

    @SerialName("guild_id")
    val guildId: Long,

    @SerialName("allow_role_id")
    val allowRoleId: List<Long> = emptyList(),

    @SerialName("submit_channel_id")
    val submitChannelId: Long,

    @SerialName("form_warning")
    val formWarning: String,

    @SerialName("form_not_you")
    val formNotYou: String,

    @SerialName("form_no_permission")
    val formNoPermission: String,

    @SerialName("form_success")
    val formSuccess: String,
)
