package tw.xinshou.loader.mongodb

import com.mongodb.client.MongoCollection
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bson.Document

/**
 * DirectCacheDb 是 ICacheDb 的實作，
 * 每次操作皆直接存取 MongoDB（不使用內存快取）。
 * 更新、刪除與清除操作以非同步方式執行，以避免阻塞呼叫端。
 */
@OptIn(DelicateCoroutinesApi::class)
class DirectCacheDb(collection: MongoCollection<Document>) : CacheCollectionManager(collection) {
    /**
     * 根據 key 插入或更新資料，以非同步方式更新 DB。
     *
     * @param key 唯一標識（自動轉換為字串）
     * @param value 要儲存的資料
     */
    override fun upsert(key: Any, value: Any) {
        GlobalScope.launch(Dispatchers.IO) {
            super.upsert(key.toString(), value)
        }
    }

    /**
     * 從 MongoDB 中移除指定 key 的資料，以非同步方式執行。
     *
     * @param key 唯一標識（自動轉換為字串）
     */
    override fun remove(key: Any) {
        GlobalScope.launch(Dispatchers.IO) {
            super.remove(key.toString())
        }
    }

    /**
     * 清除 MongoDB 中該集合的所有資料，以非同步方式執行。
     */
    override fun clear() {
        GlobalScope.launch(Dispatchers.IO) {
            super.clear()
        }
    }
}
