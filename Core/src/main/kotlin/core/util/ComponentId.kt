package core.util

class ComponentId(
    private val prefix: String,
    private val idKeys: Map<String, FieldType>,
    private val maxLength: Int = 100,
) {
    enum class FieldType { STRING, INT_HEX, LONG_HEX }

    private val keyToChar: Map<String, Char>
    private val charToKey: Map<Char, String>
    private val fieldTypes: Map<Char, FieldType>

    init {
        require(idKeys.size <= 26) { "Max 26 fields supported" }
        keyToChar = idKeys.keys.mapIndexed { i, k -> k to ('a' + i) }.toMap()
        charToKey = keyToChar.entries.associate { (k, v) -> v to k }
        fieldTypes = idKeys.entries.mapIndexed { i, (_, type) -> ('a' + i) to type }.toMap()
    }

    /**
     * Builds a component ID string from the given field pairs.
     *
     * Format: `{prefix}{char}:{value};{char}:{value};...`
     *
     * @throws IllegalArgumentException if the result exceeds [maxLength] or an unknown field is used
     */
    fun build(vararg fields: Pair<String, Any>): String {
        val sb = StringBuilder(prefix)
        for ((index, field) in fields.withIndex()) {
            val (name, value) = field
            val char = keyToChar[name]
                ?: throw IllegalArgumentException("Unknown field: $name")
            val type = idKeys[name]!!

            val encoded = when (type) {
                FieldType.STRING -> value.toString()
                FieldType.INT_HEX -> (value as Number).toInt().toString(16)
                FieldType.LONG_HEX -> (value as Number).toLong().toString(16)
            }

            sb.append(char).append(':').append(encoded)
            if (index < fields.size - 1) sb.append(';')
        }

        val result = sb.toString()
        require(result.length <= maxLength) {
            "Component ID exceeds max length of $maxLength: ${result.length} chars"
        }
        return result
    }

    /**
     * Parses a component ID string back into a map of field names to values.
     *
     * @throws IllegalArgumentException if the prefix doesn't match or format is invalid
     */
    fun parse(componentId: String): Map<String, Any> {
        require(componentId.startsWith(prefix)) {
            "Component ID does not start with expected prefix '$prefix'"
        }

        val body = componentId.removePrefix(prefix)
        if (body.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, Any>()
        val segments = body.split(';')

        for (segment in segments) {
            if (segment.isEmpty()) continue
            val colonIndex = segment.indexOf(':')
            require(colonIndex == 1) {
                "Invalid segment format: '$segment'"
            }

            val char = segment[0]
            val rawValue = segment.substring(2)
            val fieldName = charToKey[char]
                ?: throw IllegalArgumentException("Unknown field char: $char")
            val type = fieldTypes[char]!!

            val decoded: Any = when (type) {
                FieldType.STRING -> rawValue
                FieldType.INT_HEX -> rawValue.toInt(16)
                FieldType.LONG_HEX -> rawValue.toLong(16)
            }

            result[fieldName] = decoded
        }

        return result
    }
}
