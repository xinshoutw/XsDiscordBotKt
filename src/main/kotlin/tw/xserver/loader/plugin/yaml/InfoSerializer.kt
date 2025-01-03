package tw.xserver.loader.plugin.yaml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InfoSerializer(
    val author: String? = null,
    val main: String,
    val name: String,
    val description: String? = null,
    val version: String,
    val prefix: String = name,

    @SerialName("component_prefix")
    val componentPrefix: String = "",

    @SerialName("require_intents")
    val requireIntents: List<String> = emptyList(),

    @SerialName("require_cache_flags")
    val requireCacheFlags: List<String> = emptyList(),

    @SerialName("depend_plugins")
    val dependPlugins: List<String> = emptyList(),

    @SerialName("soft_depend_plugins")
    val softDependPlugins: List<String> = emptyList(),
)
