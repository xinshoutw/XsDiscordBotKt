package tw.xinshou.discord.core.mongodb

import org.bson.Document
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonFileManager.Companion.moshi

/**
 * ICacheDb 定義了快取資料存取與同步到 MongoDB 的基本操作。
 */
interface ICacheDb {
    /**
     * 取得所有快取資料的 Map，key 為字串，value 為儲存的資料。
     *
     * @return Map<String, Any>，包含所有快取資料
     */
    fun loadAll(): Map<String, Any>

    /**
     * 根據指定的 key 插入或更新資料，並同步更新至 MongoDB。
     *
     * @param key 唯一標識（Any 型別，內部會轉為字串）
     * @param value 要儲存的資料
     */
    fun upsert(key: Any, value: Any)
    fun add(key: Any, value: Any)
    fun set(key: Any, value: Any)
    fun put(key: Any, value: Any)
    fun update(key: Any, value: Any)

    /**
     * 泛型化的 get 方法，返回指定型別的結果
     *
     * @param key 唯一標識（Any 型別，內部轉成字串）
     * @return 若存在則返回對應的資料，否則返回 null
     */
    fun <T> get(key: Any): T?


    /**
     * 從快取中移除指定 key 的資料，並同步刪除 MongoDB 中的資料。
     *
     * @param key 唯一標識（Any 型別）
     */
    fun remove(key: Any)
    fun delete(key: Any)
    fun erase(key: Any)
    fun pop(key: Any)

    /**
     * 從快取中移除符合條件的資料，並同步刪除 MongoDB 中的資料。
     */
    fun <T> removeIf(predicate: (key: Any, value: Any) -> Boolean)

    /**
     * 清除所有快取資料，同時清除資料庫中該集合的所有資料。
     */
    fun clear()

    /**
     * 檢查快取中是否包含指定 key 的資料。
     */
    fun contains(key: Any): Boolean
    fun containsKey(key: Any): Boolean
    fun has(key: Any): Boolean

    /**
     * 取得快取中所有資料的筆數。
     */
    fun count(): Long
    fun size(): Long
    fun length(): Long

    /**
     * 取得快取中所有 key 的集合。
     */
    fun keys(): Set<String>
    fun keySet(): Set<String>

    /**
     * 取得集合中所有文件（完整文件）。
     */
    fun findAll(): List<Document>
}


/**
 * 泛型化的 getTyped 方法，利用 Moshi 將 Document 或其他格式轉換為指定型別。
 *
 * @param key 唯一標識
 * @return 若成功轉換則返回對應的資料，否則返回 null
 */
inline fun <reified T> ICacheDb.getTyped(key: Any): T? {
    val raw = this.get<Any>(key) ?: return null

    // 如果已經是目標型別，直接回傳
    if (raw is T) return raw

    if (raw is Document) {
        return try {
            moshi.adapterReified<T>().fromJson(raw.toJson())
        } catch (e: Exception) {
            null
        }
    }

    return try {
        val json = moshi.adapter(Any::class.java).toJson(raw)
        moshi.adapterReified<T>().fromJson(json)
    } catch (e: Exception) {
        null
    }
}
