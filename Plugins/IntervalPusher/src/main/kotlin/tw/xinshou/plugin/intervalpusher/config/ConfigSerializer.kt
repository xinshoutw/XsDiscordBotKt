package tw.xinshou.plugin.intervalpusher.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val listeners: List<Listeners>,
) {
    @Serializable
    data class Listeners(
        val url: String,
        val interval: Int,
    )
}
