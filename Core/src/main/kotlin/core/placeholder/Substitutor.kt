package tw.xinshou.discord.core.placeholder

import org.apache.commons.text.StringSubstitutor

class Substitutor(
    private val mapper: MutableMap<String, String> = mutableMapOf(),
    private val lazyMapper: MutableMap<String, () -> String> = mutableMapOf(),
    private val delimiterStart: String = "%",
    private val delimiterEnd: String = "%",
) {
    fun parse(content: String): String {
        val combined = HashMap<String, String>(mapper)
        lazyMapper.forEach { (k, v) -> combined[k] = v() }
        return StringSubstitutor(combined, delimiterStart, delimiterEnd, '$').replace(content)
    }

    fun put(key: String, value: String): Substitutor {
        mapper[key] = value
        return this
    }

    fun putAll(vararg pairs: Pair<String, String>): Substitutor {
        mapper.putAll(pairs)
        return this
    }

    fun putLazy(key: String, supplier: () -> String): Substitutor {
        lazyMapper[key] = supplier
        return this
    }
}
