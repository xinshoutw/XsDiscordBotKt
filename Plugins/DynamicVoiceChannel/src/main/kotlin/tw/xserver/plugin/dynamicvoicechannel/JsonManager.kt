package tw.xserver.plugin.dynamicvoicechannel

import com.google.gson.reflect.TypeToken
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.base.BotLoader.jdaBot
import tw.xserver.loader.json.JsonFileManager
import tw.xserver.loader.json.guild.JsonAryGuildFileManager
import tw.xserver.plugin.dynamicvoicechannel.Event.PLUGIN_DIR_FILE
import tw.xserver.plugin.dynamicvoicechannel.json.serializer.DataContainer
import tw.xserver.plugin.dynamicvoicechannel.json.serializer.JsonDataClass
import java.io.File


/*
[
  {
    categoryId: 12345678901233
    defaultName: "《🔊》新語音頻道",
    formatName1: "｜%dvc@custom-name% "
    formatName2: "｜%dvc@custom-name% ├ %dvc@info%"
  }
]
 */


internal object JsonManager {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val jsonGuildManager: JsonAryGuildFileManager = JsonAryGuildFileManager(File(PLUGIN_DIR_FILE, "data"))
    internal val dataSet: HashSet<DataContainer> = HashSet()

    init {
        jsonGuildManager.mapper.forEach { guildId, json ->
            // check guild available
            val guild: Guild? = jdaBot.getGuildById(guildId)
            if (guild == null) {
                jsonGuildManager.removeAndSave(guildId)
                return@forEach
            }

            // check json data
            var index = 0
            json.toClass<JsonDataClass>(object : TypeToken<JsonDataClass>() {}.type).forEach { data ->
                // check category available
                if (guild.getCategoryById(data.categoryId) == null) {
                    json.remove(index)
                    return@forEach
                }

                // check channel available
                var checkFlag = false
                guild.getVoiceChannelsByName(data.defaultName, false).forEach { channel ->
                    if (channel.parentCategory?.idLong == data.categoryId) {
                        checkFlag = true
                        return@forEach
                    }
                }
                if (!checkFlag) {
                    json.remove(index)
                    return@forEach
                }

                // valid data, add to map
                dataSet.add(data)

                index++
            }

            json.save()
        }
    }

    fun addData(guildId: Long, data: DataContainer) {
        val json = jsonGuildManager.get(guildId)
        json.add(JsonFileManager.gson.toJsonTree(data))
        json.save()

        dataSet.removeIf { it.categoryId == data.categoryId && it.defaultName == data.defaultName }
        dataSet.add(data)
    }

    fun removeData(guildId: Long, categoryId: Long, defaultName: String): Boolean {
        dataSet.removeIf { it.categoryId == categoryId && it.defaultName == defaultName }

        val json = jsonGuildManager.get(guildId)
        var index = 0
        json.toClass<JsonDataClass>(object : TypeToken<JsonDataClass>() {}.type).forEach { data ->
            // check category available
            if (categoryId == data.categoryId && defaultName == data.defaultName) {
                json.remove(index)
                json.save()
                return false
            }
            index++
        }

        return true
    }

    fun removeGuild(guildId: Long) {
        jsonGuildManager.removeAndSave(guildId)
    }

    fun getData(categoryId: Long, defaultName: String): DataContainer? {
        return dataSet.firstOrNull { it.categoryId == categoryId && it.defaultName == defaultName }
    }
}

