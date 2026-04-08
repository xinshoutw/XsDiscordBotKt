package core.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Simple JSON file manager using kotlinx-serialization.
 * Replaces the old Moshi-based JsonFileManager.
 */
class JsonFile<T : Any>(
    private val file: File,
    private val serializer: KSerializer<T>,
    defaultInstance: T? = null,
    private val json: Json = JsonFile.json,
) {
    var data: T = loadOrDefault(defaultInstance)
        private set

    private fun loadOrDefault(default: T?): T {
        return if (file.exists()) {
            try {
                json.decodeFromString(serializer, file.readText())
            } catch (e: Exception) {
                logger.error("Failed to parse {}: {}", file.absolutePath, e.message)
                default ?: throw e
            }
        } else {
            default ?: throw IllegalStateException("File not found and no default: ${file.absolutePath}")
        }
    }

    fun save() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(serializer, data))
    }

    fun update(block: (T) -> T) {
        data = block(data)
        save()
    }

    fun delete() {
        file.delete()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JsonFile::class.java)
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/**
 * Per-guild JSON file manager. Each guild gets its own JSON file.
 * Replaces the old Moshi-based JsonGuildFileManager.
 */
class GuildJsonFile<T : Any>(
    private val directory: File,
    private val serializer: KSerializer<T>,
    private val defaultInstance: () -> T,
    private val json: Json = JsonFile.json,
) {
    private val cache = mutableMapOf<String, JsonFile<T>>()

    operator fun get(guildId: Long): JsonFile<T> = get(guildId.toString())

    operator fun get(guildId: String): JsonFile<T> {
        return cache.getOrPut(guildId) {
            val file = File(directory, "$guildId.json")
            JsonFile(file, serializer, defaultInstance(), json)
        }
    }

    fun remove(guildId: Long) = remove(guildId.toString())

    fun remove(guildId: String) {
        cache.remove(guildId)?.delete()
            ?: File(directory, "$guildId.json").delete()
    }
}
