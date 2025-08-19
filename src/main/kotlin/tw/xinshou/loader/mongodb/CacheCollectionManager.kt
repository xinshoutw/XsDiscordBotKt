package tw.xinshou.loader.mongodb

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import org.slf4j.LoggerFactory
import tw.xinshou.loader.json.JsonFileManager


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
            val value = doc.get("value")!!
            doc.getString("_id") to deserializeValue(value)
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
        val document = collection.find(Filters.eq("_id", strKey)).firstOrNull()
        return if (document != null) {
            val value = document.get("value")!!
            deserializeValue(value) as? T
        } else {
            null
        }
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
     * 特別處理 List 類型，將其包裝在容器物件中以確保正確的 BSON 文件格式。
     *
     * @param value 要轉換的資料
     * @return 轉換後的資料
     */
    private fun serializeValue(value: Any): Any {
        return when (value) {
            is Document, is String, is Number, is Boolean -> value
            is List<*> -> {
                // Wrap List in a container object with type information to ensure proper BSON document serialization
                val itemTypeName = if (value.isNotEmpty()) {
                    value.first()?.javaClass?.name
                } else {
                    null
                }
                val wrapper = TypedListWrapper(value, itemTypeName)
                val adapter = JsonFileManager.moshi.adapter(TypedListWrapper::class.java)
                val jsonStr = adapter.toJson(wrapper)
                Document.parse(jsonStr)
            }
            else -> {
                val adapter = JsonFileManager.moshi.adapter(Any::class.java)
                val jsonStr = adapter.toJson(value)
                Document.parse(jsonStr)
            }
        }
    }

    /**
     * 將從 MongoDB 取得的資料轉換回原始格式。
     * 檢查是否為 ListWrapper 或 TypedListWrapper 物件，如果是則解包裝回 List。
     *
     * @param value 從 MongoDB 取得的資料
     * @return 解包裝後的資料
     */
    protected fun deserializeValue(value: Any): Any {
        return when (value) {
            is Document -> {
                // Check if this is a ListWrapper or TypedListWrapper by looking for the "items" field
                if (value.containsKey("items")) {
                    try {
                        // First try to deserialize as TypedListWrapper (new format with type information)
                        if (value.containsKey("itemTypeName")) {
                            val typedAdapter = JsonFileManager.moshi.adapter(TypedListWrapper::class.java)
                            val typedWrapper = typedAdapter.fromJson(value.toJson())
                            if (typedWrapper != null) {
                                return reconstructTypedList(typedWrapper)
                            }
                        }

                        // Fallback to old ListWrapper format for backward compatibility
                        val adapter = JsonFileManager.moshi.adapter(ListWrapper::class.java)
                        val wrapper = adapter.fromJson(value.toJson())
                        wrapper?.items ?: value
                    } catch (e: Exception) {
                        logger.debug("Failed to deserialize as ListWrapper, returning original value", e)
                        value
                    }
                } else {
                    value
                }
            }

            else -> value
        }
    }

    /**
     * Reconstructs a typed list from TypedListWrapper using the stored type information
     */
    private fun reconstructTypedList(wrapper: TypedListWrapper): List<*> {
        val itemTypeName = wrapper.itemTypeName
        if (itemTypeName == null || wrapper.items.isEmpty()) {
            return wrapper.items
        }

        try {
            // Get the class for the item type
            val itemClass = Class.forName(itemTypeName)

            // Create a proper adapter for the specific type
            val adapter = JsonFileManager.moshi.adapter(itemClass)

            // Reconstruct each item with proper type
            val reconstructedItems = wrapper.items.mapNotNull { item ->
                try {
                    when (item) {
                        is Map<*, *> -> {
                            // Convert the map back to JSON and then deserialize with proper type
                            val itemJson = JsonFileManager.moshi.adapter(Any::class.java).toJson(item)
                            adapter.fromJson(itemJson)
                        }

                        else -> item
                    }
                } catch (e: Exception) {
                    logger.debug("Failed to reconstruct item of type {}: {}", itemTypeName, e.message)
                    item // Return original item if reconstruction fails
                }
            }

            logger.debug("Successfully reconstructed {} items of type {}", reconstructedItems.size, itemTypeName)
            return reconstructedItems
        } catch (e: Exception) {
            logger.debug("Failed to reconstruct typed list for type {}: {}", itemTypeName, e.message)
            return wrapper.items // Return original items if reconstruction fails
        }
    }

    /**
     * Wrapper class for Lists to ensure proper BSON document serialization
     */
    private data class ListWrapper(val items: List<*>)

    /**
     * Enhanced wrapper class for Lists that includes type information for proper deserialization
     */
    private data class TypedListWrapper(val items: List<*>, val itemTypeName: String?)

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
