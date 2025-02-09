package tw.xinshou.plugin.intervalpusher.serializer

import kotlinx.serialization.Serializable

@Serializable
internal data class MainConfigSerializer(
    val listeners: List<Listeners>,
) {
    @Serializable
    data class Listeners(
        val url: String,
        val interval: Int,
    )
}
