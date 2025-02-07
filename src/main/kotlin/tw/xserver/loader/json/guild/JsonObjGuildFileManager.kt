package tw.xserver.loader.json.guild

import tw.xserver.loader.json.JsonObjFileManager
import java.io.File

class JsonObjGuildFileManager {
    private val dataDirectory: File
    val mapper: MutableMap<Long, JsonObjFileManager> = mutableMapOf()

    constructor(dataDirectory: File) {
        this.dataDirectory = dataDirectory
        if (dataDirectory.mkdirs()) {
            return
        }

        for (file in dataDirectory.listFiles().filter { it.isFile && it.extension == "json" }) {
            mapper.put(file.nameWithoutExtension.toLong(), JsonObjFileManager(file))
        }
    }

    operator fun get(guildId: Long): JsonObjFileManager {
        return mapper.getOrElse(guildId) { JsonObjFileManager(File(this.dataDirectory, "${guildId}.json")) }
    }
}
