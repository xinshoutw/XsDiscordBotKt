package tw.xserver.loader.json

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

class JsonAryFileManager(file: File) : JsonFileManager<JsonArray>(file, JsonArray::class.java) {
    override fun defaultFileAndData(): JsonArray = JsonArray()

    fun get(index: Int): JsonElement = data[index]
    fun getAsString(index: Int): String = data[index].asString
    fun getByte(index: Int): Byte = data[index].asByte
    fun getShort(index: Int): Short = data[index].asShort
    fun getInt(index: Int): Int = data[index].asInt
    fun getLong(index: Int): Long = data[index].asLong
    fun getDouble(index: Int): Double = data[index].asDouble
    fun getBoolean(index: Int): Boolean = data[index].asBoolean
    fun getJsonObject(index: Int): JsonObject = data[index].asJsonObject
    fun getJsonArray(index: Int): JsonArray = data[index].asJsonArray
    fun getBigInteger(index: Int): BigInteger = data[index].asBigInteger
    fun getBigDecimal(index: Int): BigDecimal = data[index].asBigDecimal
    fun getJsonPrimitive(index: Int): JsonPrimitive = data[index].asJsonPrimitive
    fun getNumber(index: Int): Number = data[index].asNumber

    fun forEach(action: (JsonElement) -> Unit) = data.forEach(action)

    fun add(number: Number): JsonAryFileManager {
        data.add(number)
        return this
    }

    fun add(string: String): JsonAryFileManager {
        data.add(string)
        return this
    }

    fun add(bool: Boolean): JsonAryFileManager {
        data.add(bool)
        return this
    }

    fun add(character: Char): JsonAryFileManager {
        data.add(character)
        return this
    }

    fun add(element: JsonElement): JsonAryFileManager {
        data.add(element)
        return this
    }

    fun addAll(jsonArray: JsonArray): JsonAryFileManager {
        data.addAll(jsonArray)
        return this
    }

    fun remove(index: Int): Boolean =
        if (index in 0 until data.size()) {
            data.remove(index)
            true
        } else {
            false
        }
}
