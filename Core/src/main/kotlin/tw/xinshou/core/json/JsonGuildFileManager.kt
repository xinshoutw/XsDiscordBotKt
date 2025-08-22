package tw.xinshou.core.json

import com.squareup.moshi.JsonAdapter
import java.io.File
import java.nio.file.Files

class JsonGuildFileManager<T>(
    private val dataDirectory: File,
    private val adapter: JsonAdapter<T>,
    private val defaultInstance: T? = null
) {
    val mapper: MutableMap<Long, JsonFileManager<T>> = mutableMapOf()

    init {
        Files.createDirectories(dataDirectory.toPath())
        for (file in dataDirectory.listFiles().filter { it.isFile && it.extension == "json" }) {
            mapper.put(file.nameWithoutExtension.toLong(), JsonFileManager(file, adapter, defaultInstance))
        }
    }

    operator fun get(guildId: Long): JsonFileManager<T> {
        val manager = mapper.getOrPut(guildId) {
            JsonFileManager<T>(
                File(this.dataDirectory, "${guildId}.json"),
                adapter,
                defaultInstance
            )
        }

        if (manager.isDeleted) {
            mapper.remove(guildId)
            return get(guildId)
        }

        return manager
    }

    fun removeAndSave(guildId: Long) {
        mapper.remove(guildId)?.delete()
    }
}