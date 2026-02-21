package tw.xinshou.discord.core.builtin.placeholder

import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup

class Substitutor(
    private val mapper: MutableMap<String, String> = HashMap(),
    private val lazyMapper: MutableMap<String, () -> String> = HashMap(),
    private val delimiterStart: String = "%",
    private val delimiterEnd: String = "%",
    private val escape: Char = '$'
) {
    private var substitutor = createSubstitutor()

    // Convenience constructor to initialize with pairs
    constructor(vararg pairs: Pair<String, String>) :
            this(pairs.toMap().toMutableMap())

    // Constructor to inherit and add from another Substitutor
    constructor(parent: Substitutor, vararg pairs: Pair<String, String>) :
            this(pairs.toMap().toMutableMap()) {
        addAll(parent) // refresh
    }

    // Replace placeholders in the content string using the current substitutor
    fun parse(content: String): String = substitutor.replace(content)

    // Retrieve the value for a key or return the key itself if not found
    fun get(key: String): String = resolveValue(key) ?: key

    // Add all mappings from another Substitutor
    fun addAll(substitutor: Substitutor): Substitutor = apply {
        mapper.putAll(substitutor.mapper)
        lazyMapper.putAll(substitutor.lazyMapper)
        refreshSubstitutor()
    }

    // Add a single pair to the map
    fun put(pair: Pair<String, String>): Substitutor = apply {
        mapper[pair.first] = pair.second
        lazyMapper.remove(pair.first)
        refreshSubstitutor()
    }

    // Put a single key-value pair into the map
    fun put(key: String, value: String): Substitutor = apply {
        mapper[key] = value
        lazyMapper.remove(key)
        refreshSubstitutor()
    }

    // Put a lazy supplier for a key. Value will only be resolved when key is used.
    fun putLazy(key: String, supplier: () -> String): Substitutor = apply {
        lazyMapper[key] = supplier
        mapper.remove(key)
        refreshSubstitutor()
    }

    // Add multiple pairs to the map
    fun putAll(vararg pairs: Pair<String, String>): Substitutor = apply {
        pairs.forEach {
            mapper[it.first] = it.second
            lazyMapper.remove(it.first)
        }
        refreshSubstitutor()
    }

    // Put all key-value pairs from a map into the map
    fun putAll(kv: Map<String, String>): Substitutor = apply {
        kv.forEach { (key, value) ->
            mapper[key] = value
            lazyMapper.remove(key)
        }
        refreshSubstitutor()
    }

    // Put all lazy key-supplier mappings
    fun putAllLazy(kv: Map<String, () -> String>): Substitutor = apply {
        kv.forEach { (key, supplier) ->
            lazyMapper[key] = supplier
            mapper.remove(key)
        }
        refreshSubstitutor()
    }

    // Refresh the internal StringSubstitutor instance to reflect the current map state
    private fun refreshSubstitutor() {
        substitutor = createSubstitutor()
    }

    // Create a new StringSubstitutor with current settings
    private fun createSubstitutor() =
        StringSubstitutor(
            StringLookup { key -> key?.let { resolveValue(it) } },
            delimiterStart,
            delimiterEnd,
            escape
        )

    private fun resolveValue(key: String): String? = lazyMapper[key]?.invoke() ?: mapper[key]
}
