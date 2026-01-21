package tw.xinshou.discord.plugin.dynamicvoicechannel

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.BotLoader.jdaBot
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.plugin.dynamicvoicechannel.Event.pluginDirectory
import tw.xinshou.discord.plugin.dynamicvoicechannel.json.DataContainer
import tw.xinshou.discord.plugin.dynamicvoicechannel.json.JsonDataClass
import java.io.File
import java.util.concurrent.ConcurrentHashMap


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

    private val jsonAdapter: JsonAdapter<JsonDataClass> =
        JsonFileManager.moshi.adapterReified<JsonDataClass>()

    private val jsonGuildManager = JsonGuildFileManager(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = mutableListOf()
    )

    private val dataSet: MutableSet<DataContainer> = ConcurrentHashMap.newKeySet()

    init {
        try {
            logger.info("[JsonManager] Initializing DynamicVoiceChannel data...")

            jsonGuildManager.mapper.forEach { (guildId, jsonManager) ->
                val guild: Guild? = jdaBot.getGuildById(guildId)

                if (guild == null) {
                    logger.warn("Guild $guildId not found, removing associated data file.")
                    jsonGuildManager.removeAndSave(guildId)
                    return@forEach
                }

                val validData = jsonManager.data.filter { data ->
                    val categoryExists = guild.getCategoryById(data.categoryId) != null
                    if (!categoryExists) {
                        logger.warn("Category ${data.categoryId} not found in guild ${guild.name}, skipping.")
                        return@filter false
                    }

                    val channelExists = guild
                        .getVoiceChannelsByName(data.defaultName, false)
                        .any { it.parentCategory?.idLong == data.categoryId }

                    if (!channelExists) {
                        logger.warn("Channel ${data.defaultName} not found under category ${data.categoryId}, skipping.")
                        return@filter false
                    }

                    // 如果通過檢查，加入 dataSet
                    dataSet.add(data)
                    true
                }

                // 將過濾後的資料覆蓋回 JSON
                jsonManager.data = validData.toMutableList()
                jsonManager.save()
            }

            logger.info("[JsonManager] Initialization complete. Loaded ${dataSet.size} valid data entries.")
        } catch (e: Exception) {
            logger.error("[JsonManager] Initialization failed: ${e.message}", e)
        }
    }

    fun addData(guildId: Long, data: DataContainer) {
        val json = jsonGuildManager.get(guildId)
        synchronized(json) {
            json.data.add(data)
            json.save()
        }

        synchronized(dataSet) {
            dataSet.removeIf { it.categoryId == data.categoryId && it.defaultName == data.defaultName }
            dataSet.add(data)
        }

        logger.info("Added DynamicVoiceChannel binding for guild $guildId → ${data.defaultName}")
    }

    fun removeData(guildId: Long, categoryId: Long, defaultName: String): Boolean {
        synchronized(dataSet) {
            dataSet.removeIf { it.categoryId == categoryId && it.defaultName == defaultName }
        }

        val jsonManager = jsonGuildManager.get(guildId)
        var removed = false

        synchronized(jsonManager) {
            val newData = jsonManager.data.filterNot {
                it.categoryId == categoryId && it.defaultName == defaultName
            }
            removed = newData.size != jsonManager.data.size
            if (removed) {
                jsonManager.data = newData.toMutableList()
                jsonManager.save()
            }
        }

        if (removed) {
            logger.info("Removed DynamicVoiceChannel binding for guild $guildId → $defaultName")
        }

        return !removed
    }

    fun removeGuild(guildId: Long) {
        jsonGuildManager.removeAndSave(guildId)
        synchronized(dataSet) {
            dataSet.removeIf { it.categoryId.toString().startsWith(guildId.toString()) }
        }
        logger.info("Removed all DynamicVoiceChannel data for guild $guildId")
    }

    fun getData(categoryId: Long, defaultName: String): DataContainer? {
        synchronized(dataSet) {
            return dataSet.firstOrNull { it.categoryId == categoryId && it.defaultName == defaultName }
        }
    }
}

