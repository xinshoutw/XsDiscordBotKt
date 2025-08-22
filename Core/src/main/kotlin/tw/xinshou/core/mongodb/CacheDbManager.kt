package tw.xinshou.core.mongodb

import com.mongodb.client.MongoDatabase

/**
 * CacheDbManager 管理插件專屬的資料庫快取，
 * 可透過此類別取得指定集合的快取實作（記憶體快取或直接 DB）。
 *
 * @param databaseName 插件使用的資料庫名稱
 */
class CacheDbManager(
    private val databaseName: String,
) {
    private val database: MongoDatabase = CacheDbServer.getDatabase(databaseName)

    /**
     * 取得指定集合的快取實作。
     *
     * @param collectionName 集合名稱
     * @param memoryCache 若為 true，返回 MemoryCacheDb；否則返回 DirectCacheDb
     * @return ICacheDb 實例
     */
    fun getCollection(collectionName: String, memoryCache: Boolean = false): ICacheDb {
        return if (memoryCache) {
            MemoryCacheDb(database.getCollection(collectionName))
        } else {
            DirectCacheDb(database.getCollection(collectionName))
        }
    }

    /**
     * 列出此資料庫中所有集合的名稱。
     *
     * @return 集合名稱列表
     */
    fun listCollections(): List<String> {
        return database.listCollectionNames().toList()
    }

    /**
     * 刪除此資料庫（會清除所有資料），請小心使用。
     */
    fun dropDatabase() {
        database.drop()
    }
}