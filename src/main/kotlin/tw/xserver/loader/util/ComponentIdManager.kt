package tw.xserver.loader.util

import net.dv8tion.jda.api.interactions.components.buttons.Button.ID_MAX_LENGTH

/**
 * 定義欄位型別:
 *   STRING   -> 不壓縮，直接存放原字串
 *   INT_HEX  -> 存放時轉為 16 進位字串；解析時還原為 Int
 *   LONG_HEX -> 存放時轉為 16 進位字串；解析時還原為 Long
 */
enum class FieldType {
    STRING,
    INT_HEX,
    LONG_HEX,
}

/**
 * 代表一個欄位的結構：
 *  - name:   欄位名稱
 *  - value:  欲存放的資料 (String, Int, 或 Long)
 */
data class ComponentField(
    val name: String,
    val value: Any,
)

/**
 * 用於建構/解析 Discord componentId。
 *
 * 最終字串格式示例:
 *   {prefix}@{key1}:{value1};{key2}:{value2};...
 *
 * 其中 key1, key2 為由 "(原始名稱, FieldType)" 壓縮成的單字元 (a, b, c...),
 * value1, value2 則可能是原字串 / 十六進位字串，視 type 而定。
 *
 * @param prefix     componentId 前綴，不可包含 '@'
 * @param idKeys     可用於辨識的欄位名稱與其型別，舉例:
 *                   mapOf("msgId" to FieldType.INT_HEX, "username" to FieldType.STRING)
 * @param separator  key:value 與 key:value 之間的分隔符號 (預設 ";")
 * @param maxLength  componentId 最大長度限制，預設使用 JDA Button 的限制
 *
 * 初始化後會建立一個 keyMapper (Map<Any, Any>):
 *   Pair("msgId", FieldType.INT_HEX) -> "a"
 *   "a" -> Pair("msgId", FieldType.INT_HEX)
 *   ...
 */
class ComponentIdManager(
    private val prefix: String,                     // 不可包含 '@'
    private val idKeys: Map<String, FieldType>,     // 允許使用的欄位名稱與型別
    private val separator: String = ";",            // key:value 與 key:value 間的分隔符號
    private val maxLength: Int = ID_MAX_LENGTH,
) {

    /**
     * keyMapper 用於雙向映射：
     *   (原始名稱, FieldType) -> "a"
     *   "a" -> (原始名稱, FieldType)
     *
     *   (原始名稱2, FieldType2) -> "b"
     *   "b" -> (原始名稱2, FieldType2)
     *   ...
     */
    private val keyMapper: Map<Any, Any>

    init {
        require(prefix.isNotBlank()) {
            "prefix must not be blank."
        }
        require(!prefix.contains("@")) {
            "prefix must not contain '@'."
        }
        require(idKeys.isNotEmpty()) {
            "idKeys cannot be empty."
        }
        require(idKeys.size <= 26) {
            "idKeys size must not exceed 26 (預設示例: 單字元只能覆蓋 a~z)"
        }

        keyMapper = idKeys.entries.flatMapIndexed { index, (originalName, fieldType) ->
            val shortKey = ('a' + index).toString() // 單字元
            listOf(
                // 正向
                originalName to shortKey,
                // 反向
                shortKey to Pair(originalName, fieldType)
            )
        }.toMap()
    }

    // --------------------------------------------------
    // Public API
    // --------------------------------------------------

    /**
     * 建構一條 componentId 字串，格式:
     *   {prefix}@{shortKey}:{value};{shortKey}:{value};...
     *
     * @param fields 多個 ComponentField (vararg)
     *
     * @return 組裝完成後的 componentId，例如 "ticket@a:3e8;b:John"
     *
     * @throws IllegalArgumentException 若:
     *   1. 欄位名稱不在 idKeys 裡面
     *   2. 產生的字串超過 maxLength
     */
    fun build(vararg fields: ComponentField): String {
        // 1) 先轉成「(名稱, 型別)」並取得對應 shortKey
        val pairs = fields.map { cf ->
            val fieldType = idKeys[cf.name] ?: error("Invalid field name '${cf.name}' - not in idKeys.")
            val shortKey = keyMapper[cf.name] as? String
                ?: error("No mapped shortKey for field '${cf.name}' with type '$fieldType'")

            // 2) 檢查型別 & 轉成字串
            checkValueType(cf.name, cf.value)
            val encodedValue = convertToString(fieldType, cf.value)

            "$shortKey:$encodedValue"
        }

        // 3) 組合最終字串 => prefix@a:xxx;b:yyy
        val finalString = "$prefix@${pairs.joinToString(separator)}"
        require(finalString.length <= maxLength) {
            "componentId 超過長度限制: ${finalString.length} (最大 $maxLength)"
        }
        return finalString
    }

    /**
     * 解析 componentId，還原為 Map<String, Any>，
     * 其中 key 為「原始欄位名稱」，value 為還原後的資料 (String / Int / Long)。
     *
     * @param componentId 形如 "ticket@a:3e8;b:John;..." 的字串
     * @return 解析後的 Map，例如 { "msgId"=1000, "username"="John", ... }
     *
     * @throws IllegalArgumentException 若前綴不符、短字元無法對應、字串格式錯誤、或超過長度
     */
    fun parse(componentId: String): Map<String, Any> {
        require(componentId.isNotBlank()) { "componentId must not be blank." }
        require(componentId.length <= maxLength) {
            "componentId must not exceed $maxLength, actual length: ${componentId.length}."
        }
        require(componentId.startsWith("$prefix@")) {
            "componentId must start with '$prefix@'."
        }

        val body = componentId.removePrefix("$prefix@")
        if (body.isBlank()) return emptyMap()

        // 拆分成多個 "key:value"
        val segments = body.split(separator)
        require(segments.isNotEmpty()) {
            "No fields found after prefix."
        }

        val resultMap = mutableMapOf<String, Any>()
        for (seg in segments) {
            val token = seg.split(":")
            require(token.size == 2) {
                "Invalid segment format: '$seg' (must be 'shortKey:encodedValue')"
            }
            val shortKey = token[0]
            val encodedValue = token[1]

            @Suppress("UNCHECKED_CAST")
            val pair: Pair<String, FieldType> = keyMapper[shortKey] as? Pair<String, FieldType>
                ?: error("ShortKey '$shortKey' is invalid or not in keyMapper.")

            val (originalName, fieldType) = pair
            val parsedValue = convertFromString(fieldType, encodedValue)
            resultMap[originalName] = parsedValue
        }

        return resultMap
    }

    // --------------------------------------------------
    // Private Helpers
    // --------------------------------------------------

    /**
     * 檢查「該 name 對應的型別」與實際傳入的 value 是否相符
     *
     * 這裡根據 idKeys[name] 取得應該的 FieldType，
     * 然後進行 require() 檢查。
     */
    private fun checkValueType(name: String, value: Any) {
        val expectedType = idKeys[name]
            ?: error("Field '$name' not found in idKeys (should never happen if we checked earlier).")

        when (expectedType) {
            FieldType.STRING -> {
                require(value is String) {
                    "欄位 '$name' 預期為 String，卻得到 ${value.javaClass.simpleName}"
                }
            }

            FieldType.INT_HEX -> {
                require(value is Int) {
                    "欄位 '$name' 預期為 Int，卻得到 ${value.javaClass.simpleName}"
                }
            }

            FieldType.LONG_HEX -> {
                require(value is Long) {
                    "欄位 '$name' 預期為 Long，卻得到 ${value.javaClass.simpleName}"
                }
            }
        }
    }

    /**
     * 將 value 轉為字串 (STRING 或 16進位)
     */
    private fun convertToString(type: FieldType, value: Any): String =
        when (type) {
            FieldType.STRING -> value as String
            FieldType.INT_HEX -> (value as Int).toString(16)
            FieldType.LONG_HEX -> (value as Long).toString(16)
        }

    /**
     * 從字串 (16進位或原字串) 還原為實際型別
     */
    private fun convertFromString(type: FieldType, token: String): Any =
        when (type) {
            FieldType.STRING -> token
            FieldType.INT_HEX -> token.toInt(16)
            FieldType.LONG_HEX -> token.toLong(16)
        }
}
