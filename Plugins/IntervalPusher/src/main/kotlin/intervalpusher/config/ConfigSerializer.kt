package tw.xinshou.discord.plugin.intervalpusher.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
    val listeners: List<Listeners>,
) {
    @Serializable
    data class Listeners(
        val url: String,
        val interval: Int,
    )
}
