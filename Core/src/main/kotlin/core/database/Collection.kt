package tw.xinshou.discord.core.database

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class Collection<T : Any>(
    private val collection: MongoCollection<T>
) {
    private val upsertOptions = ReplaceOptions().upsert(true)

    suspend fun get(key: String): T? {
        return collection.find(Filters.eq("_id", key)).firstOrNull()
    }

    suspend fun upsert(key: String, value: T) {
        collection.replaceOne(Filters.eq("_id", key), value, upsertOptions)
    }

    suspend fun delete(key: String) {
        collection.deleteOne(Filters.eq("_id", key))
    }

    suspend fun findAll(): List<T> {
        return collection.find().toList()
    }

    suspend fun count(): Long {
        return collection.countDocuments()
    }

    suspend fun contains(key: String): Boolean {
        return collection.countDocuments(Filters.eq("_id", key)) > 0
    }

    suspend fun clear() {
        collection.drop()
    }
}
