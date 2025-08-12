package tw.xinshou.plugin.giveaway.config

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
