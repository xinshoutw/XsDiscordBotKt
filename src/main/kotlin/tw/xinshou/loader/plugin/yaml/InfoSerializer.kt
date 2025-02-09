package tw.xinshou.loader.plugin.yaml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.utils.MemberCachePolicy

@Serializable
internal data class InfoSerializer(
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

    @SerialName("require_member_cache_policies")
    val requireMemberCachePolicies: List<String> = emptyList(),

    @SerialName("depend_plugins")
    val dependPlugins: List<String> = emptyList(),

    @SerialName("soft_depend_plugins")
    val softDependPlugins: List<String> = emptyList(),
)

fun processMemberCachePolicy(list: List<String>): MemberCachePolicy {
    // OWNER
    // ONLINE
    // VOICE
    // BOOSTER
    // PENDING

    var cur = MemberCachePolicy.OWNER
    for (policy in list) {
        cur = when (policy) {
            "OWNER" -> cur.or(MemberCachePolicy.OWNER)
            "ONLINE" -> cur.or(MemberCachePolicy.ONLINE)
            "VOICE" -> cur.or(MemberCachePolicy.VOICE)
            "BOOSTER" -> cur.or(MemberCachePolicy.BOOSTER)
            "PENDING" -> cur.or(MemberCachePolicy.PENDING)
            else -> cur
        }
    }
    return cur
}