package core.plugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.utils.MemberCachePolicy

@Serializable
data class PluginConfig(
    val author: String = "",
    val main: String,
    val name: String,
    val description: String = "",
    @SerialName("coreApi") val coreApi: String,
    val version: String,
    @SerialName("component_prefix") val componentPrefix: String = "",
    @SerialName("require_intents") val requireIntents: List<String> = emptyList(),
    @SerialName("require_cache_flags") val requireCacheFlags: List<String> = emptyList(),
    @SerialName("require_member_cache_policies") val requireMemberCachePolicies: List<String> = emptyList(),
    @SerialName("depend_plugins") val dependPlugins: List<String> = emptyList(),
    @SerialName("soft_depend_plugins") val softDependPlugins: List<String> = emptyList(),
)

fun processMemberCachePolicy(policies: List<String>): MemberCachePolicy {
    if (policies.isEmpty()) return MemberCachePolicy.NONE

    val resolved = policies.map { policy ->
        when (policy.uppercase()) {
            "VOICE" -> MemberCachePolicy.VOICE
            "ONLINE" -> MemberCachePolicy.ONLINE
            "OWNER" -> MemberCachePolicy.OWNER
            "PENDING" -> MemberCachePolicy.PENDING
            else -> error("Unknown MemberCachePolicy: $policy")
        }
    }

    return MemberCachePolicy.any(resolved.first(), *resolved.drop(1).toTypedArray())
}
