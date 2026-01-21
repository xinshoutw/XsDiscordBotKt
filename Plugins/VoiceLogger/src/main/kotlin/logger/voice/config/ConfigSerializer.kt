package tw.xinshou.discord.plugin.logger.voice.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
)
