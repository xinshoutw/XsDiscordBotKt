package tw.xinshou.core.plugin.yaml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.utils.MemberCachePolicy

@Serializable
internal data class InfoSerializer(
    val author: String? = null,
    val main: String,
    val name: String,
    val description: String? = null,
    val coreApi: String,
    val version: String,
    val prefix: String = name,

    @SerialName("component_prefix")
    val componentPrefix: String = "",

    @SerialName("require_intents")
    val requireIntents: Set<String> = emptySet(),

    @SerialName("require_cache_flags")
    val requireCacheFlags: Set<String> = emptySet(),

    @SerialName("require_member_cache_policies")
    val requireMemberCachePolicies: Set<String> = emptySet(),

    @SerialName("depend_plugins")
    val dependPlugins: Set<String> = emptySet(),

    @SerialName("soft_depend_plugins")
    val softDependPlugins: Set<String> = emptySet(),
)

fun processMemberCachePolicy(list: Set<String>): MemberCachePolicy {
    // OWNER
    // ONLINE
    // VOICE
    // BOOSTER
    // PENDING
    // ALL

    if (list.contains("ALL")) {
        return MemberCachePolicy.ALL
    }

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