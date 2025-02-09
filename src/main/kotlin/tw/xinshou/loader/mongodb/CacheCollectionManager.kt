package tw.xinshou.loader.mongodb

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.bson.Document
import org.slf4j.LoggerFactory


/**
 * CacheCollectionManager 提供操作單一集合的基本方法，
 * 包含插入/更新（upsert）、讀取、刪除、清除資料，以及
 * 檢查資料是否存在、取得文件數量、列出所有 key 與更新單一欄位的功能。
 *
 * @param collection 對應的 MongoCollection<Document> 實例
 */
open class CacheCollectionManager(private val collection: MongoCollection<Document>) : ICacheDb {
    /**
     * 從 MongoDB 中取得所有資料，並組成 Map（key 為 _id）。
     *
     * @return Map<String, Any>
     */
    override fun loadAll(): Map<String, Any> {
        return findAll().associate { doc ->
            doc.getString("_id") to doc.get("value")!!
        }
    }

    override fun upsert(key: Any, value: Any) {
        val strKey = key.toString()
        val bsonValue = serializeValue(value)
        val doc = Document("_id", strKey)
            .append("value", bsonValue)
            .append("timestamp", System.currentTimeMillis())
        collection.replaceOne(Filters.eq("_id", strKey), doc, ReplaceOptions().upsert(true))
    }

    override fun add(key: Any, value: Any) = upsert(key, value)
    override fun set(key: Any, value: Any) = upsert(key, value)
    override fun put(key: Any, value: Any) = upsert(key, value)
    override fun update(key: Any, value: Any) = upsert(key, value)


    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Any): T? {
        val strKey = key.toString()
        return collection.find(Filters.eq("_id", strKey)).firstOrNull()?.get("value") as? T
    }


    override fun remove(key: Any) {
        val strKey = key.toString()
        collection.deleteOne(Filters.eq("_id", strKey))
    }

    override fun delete(key: Any) = remove(key)
    override fun erase(key: Any) = remove(key)
    override fun pop(key: Any) = remove(key)

    override fun <T> removeIf(predicate: (key: Any, value: Any) -> Boolean) {
        keys().forEach { key ->
            get<T>(key)?.let {
                if (predicate(key, it)) {
                    remove(key)
                }
            }
        }
    }

    /**
     * 清除整個集合的所有資料。
     */
    override fun clear() {
        collection.drop()
    }

    /**
     * 檢查集合中是否存在指定 key 的資料。
     *
     * @param key 唯一標識（會自動轉換為字串）
     * @return 存在則返回 true，否則返回 false
     */
    override fun contains(key: Any): Boolean {
        val strKey = key.toString()
        return collection.find(Filters.eq("_id", strKey)).firstOrNull() != null
    }

    override fun containsKey(key: Any): Boolean = contains(key)
    override fun has(key: Any): Boolean = contains(key)

    /**
     * 取得集合中所有文件的筆數。
     *
     * @return 文件數量
     */
    override fun count(): Long {
        return collection.countDocuments()
    }

    override fun size(): Long = count()
    override fun length(): Long = count()

    /**
     * 列出集合中所有文件的 key。
     *
     * @return key 的字串列表
     */
    override fun keys(): Set<String> {
        return collection.find().mapNotNull { it.getString("_id") }.toSet()
    }

    override fun keySet(): Set<String> = keys()

    /**
     * 取得集合中所有文件（完整文件）。
     *
     * @return List<Document> 文件列表
     */
    override fun findAll(): List<Document> {
        return collection.find().toList()
    }

    /**
     * 將傳入的 value 轉換為 MongoDB 可接受的格式。
     * 若 value 為 Document、String、Number 或 Boolean 則直接使用，
     * 否則使用 Moshi 將物件序列化成 JSON 字串，再解析為 Document。
     *
     * @param value 要轉換的資料
     * @return 轉換後的資料
     */
    private fun serializeValue(value: Any): Any {
        return when (value) {
            is Document, is String, is Number, is Boolean -> value
            else -> {
                val adapter = moshi.adapter(Any::class.java)
                val jsonStr = adapter.toJson(value)
                Document.parse(jsonStr)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
}
