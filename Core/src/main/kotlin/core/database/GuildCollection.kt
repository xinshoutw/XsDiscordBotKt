package tw.xinshou.discord.core.database

import com.mongodb.kotlin.client.coroutine.MongoDatabase

class GuildCollection<T : Any>(
    private val database: MongoDatabase,
    private val collectionPrefix: String,
    private val codec: Class<T>,
) {
    fun forGuild(guildId: Long): Collection<T> {
        return forGuild(guildId.toString())
    }

    fun forGuild(guildId: String): Collection<T> {
        val name = "${collectionPrefix}_$guildId"
        val mongoCollection = database.getCollection(name, codec)
        return Collection(mongoCollection)
    }
}
