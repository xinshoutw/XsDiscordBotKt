package tw.xserver.loader.json.guild

import tw.xserver.loader.json.JsonAryFileManager
import java.io.File

class JsonAryGuildFileManager {
    private val dataDirectory: File
    val mapper: MutableMap<Long, JsonAryFileManager> = mutableMapOf()

    constructor(dataDirectory: File) {
        this.dataDirectory = dataDirectory
        if (dataDirectory.mkdirs()) {
            return
        }

        for (file in dataDirectory.listFiles().filter { it.isFile && it.extension == "json" }) {
            mapper.put(file.nameWithoutExtension.toLong(), JsonAryFileManager(file))
        }
    }

    operator fun get(guildId: Long): JsonAryFileManager {
        return mapper.getOrElse(guildId) { JsonAryFileManager(File(this.dataDirectory, "${guildId}.json")) }
    }

    @Synchronized
    fun removeAndSave(guildId: Long) {
        mapper.remove(guildId)
        dataDirectory.resolve("${guildId}.json").delete()
    }
}
