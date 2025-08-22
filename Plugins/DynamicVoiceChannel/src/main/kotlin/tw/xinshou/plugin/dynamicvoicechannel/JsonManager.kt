package tw.xinshou.plugin.dynamicvoicechannel

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.core.base.BotLoader.jdaBot
import tw.xinshou.core.json.JsonFileManager
import tw.xinshou.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.core.json.JsonGuildFileManager
import tw.xinshou.plugin.dynamicvoicechannel.Event.pluginDirectory
import tw.xinshou.plugin.dynamicvoicechannel.json.DataContainer
import tw.xinshou.plugin.dynamicvoicechannel.json.JsonDataClass
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
    val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()
    val jsonGuildManager = JsonGuildFileManager<JsonDataClass>(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = mutableListOf()
    )

    private val dataSet: HashSet<DataContainer> = HashSet()

    init {
        jsonGuildManager.mapper.forEach { guildId, jsonManager ->
            // check guild available
            val guild: Guild? = jdaBot.getGuildById(guildId)
            if (guild == null) {
                jsonGuildManager.removeAndSave(guildId)
                return@forEach
            }

            // check json data
            var index = 0
            jsonManager.data.forEach { data ->
                // check category available
                if (guild.getCategoryById(data.categoryId) == null) {
                    jsonManager.data.removeAt(index)
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
                    jsonManager.data.removeAt(index)
                    return@forEach
                }

                // valid data, add to map
                dataSet.add(data)

                index++
            }

            jsonManager.save()
        }
    }

    fun addData(guildId: Long, data: DataContainer) {
        val json = jsonGuildManager.get(guildId)
        json.data.add(data)
        json.save()

        dataSet.removeIf { it.categoryId == data.categoryId && it.defaultName == data.defaultName }
        dataSet.add(data)
    }

    fun removeData(guildId: Long, categoryId: Long, defaultName: String): Boolean {
        dataSet.removeIf { it.categoryId == categoryId && it.defaultName == defaultName }

        val jsonManager = jsonGuildManager.get(guildId)
        var index = 0
        jsonManager.data.forEach { data ->
            // check category available
            if (categoryId == data.categoryId && defaultName == data.defaultName) {
                jsonManager.data.removeAt(index)
                jsonManager.save()
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

