package tw.xserver.loader.mongodb

import com.mongodb.client.MongoCollection
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

/**
 * MemoryCacheDb 是 ICacheDb 的實作，
 * 初始化時會從 MongoDB 載入所有資料存入內存快取，
 * 查詢時直接從內存讀取，
 * 更新、刪除、清除操作則以非同步方式同步更新至 MongoDB，
 * 以避免阻塞主要邏輯。
 *
 * @param collection MongoCollection<Document>
 */
@OptIn(DelicateCoroutinesApi::class)
class MemoryCacheDb(collection: MongoCollection<Document>) : CacheCollectionManager(collection) {
    private val cache: ConcurrentHashMap<String, Any> = ConcurrentHashMap()

    init {
        // 初始化時從 DB 載入所有資料到內存快取
        val allDocs: List<Document> = findAll()
        for (doc in allDocs) {
            val key = doc.getString("_id")
            val value = doc.get("value")
            if (key != null && value != null) {
                cache[key] = value
            }
        }
    }

    /**
     * 取得內部快取中所有資料的 Map。
     *
     * @return Map<String, Any>，包含所有快取資料
     */
    override fun loadAll(): Map<String, Any> {
        return cache.toMap()
    }

    /**
     * 根據 key 插入或更新資料，先更新內存快取，再以非同步方式更新 MongoDB。
     *
     * @param key 唯一標識（自動轉換為字串）
     * @param value 要儲存的資料
     */
    override fun upsert(key: Any, value: Any) {
        val strKey = key.toString()
        cache[strKey] = value
        GlobalScope.launch(Dispatchers.IO) {
            super.upsert(strKey, value)
        }
    }

    /**
     * 從內部快取中取得指定 key 的資料。
     *
     * @param key 唯一標識（自動轉換為字串）
     * @return 對應的資料，若不存在則返回 null
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Any): T? {
        return cache[key.toString()] as T?
    }

    /**
     * 從內部快取中移除指定 key 的資料，並以非同步方式刪除 MongoDB 中的資料。
     *
     * @param key 唯一標識（自動轉換為字串）
     */
    override fun remove(key: Any) {
        val strKey = key.toString()
        cache.remove(strKey)
        GlobalScope.launch(Dispatchers.IO) {
            super.remove(strKey)
        }
    }

    /**
     * 清除內部快取與 MongoDB 中的所有資料，以非同步方式執行 DB 清除。
     */
    override fun clear() {
        cache.clear()
        GlobalScope.launch(Dispatchers.IO) {
            super.clear()
        }
    }
}
