package tw.xinshou.discord.plugin.welcomebyeguild.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
