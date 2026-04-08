package tw.xinshou.discord.plugin.dynamicvoicechannel

import tw.xinshou.discord.core.util.GuildJsonFile
import kotlinx.serialization.builtins.ListSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.plugin.dynamicvoicechannel.Event.pluginDirectory
import tw.xinshou.discord.plugin.dynamicvoicechannel.json.DataContainer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal object JsonManager {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val jsonGuildManager = GuildJsonFile(
        directory = File(pluginDirectory, "data"),
        serializer = ListSerializer(DataContainer.serializer()),
        defaultInstance = { mutableListOf() },
    )

    private val dataSet: MutableSet<DataContainer> = ConcurrentHashMap.newKeySet()

    init {
        try {
            logger.info("[JsonManager] Initializing DynamicVoiceChannel data...")

            val dataFolder = File(pluginDirectory, "data")
            dataFolder.listFiles()?.filter { it.isFile && it.extension == "json" }?.forEach { file ->
                val guildId = file.nameWithoutExtension
                try {
                    val jsonFile = jsonGuildManager[guildId]
                    jsonFile.data.forEach { data ->
                        dataSet.add(data)
                    }
                } catch (e: Exception) {
                    logger.error("Error loading data for guild {}: {}", guildId, e.message)
                }
            }

            logger.info("[JsonManager] Initialization complete. Loaded ${dataSet.size} valid data entries.")
        } catch (e: Exception) {
            logger.error("[JsonManager] Initialization failed: ${e.message}", e)
        }
    }

    fun addData(guildId: Long, data: DataContainer) {
        val json = jsonGuildManager[guildId]
        synchronized(json) {
            @Suppress("UNCHECKED_CAST")
            val list = json.data as? MutableList<DataContainer> ?: return
            list.add(data)
            json.save()
        }

        synchronized(dataSet) {
            dataSet.removeIf { it.categoryId == data.categoryId && it.defaultName == data.defaultName }
            dataSet.add(data)
        }

        logger.info("Added DynamicVoiceChannel binding for guild $guildId -> ${data.defaultName}")
    }

    fun removeData(guildId: Long, categoryId: Long, defaultName: String): Boolean {
        synchronized(dataSet) {
            dataSet.removeIf { it.categoryId == categoryId && it.defaultName == defaultName }
        }

        val jsonFile = jsonGuildManager[guildId]
        var removed = false

        synchronized(jsonFile) {
            @Suppress("UNCHECKED_CAST")
            val list = jsonFile.data as? MutableList<DataContainer> ?: return true
            val newData = list.filterNot {
                it.categoryId == categoryId && it.defaultName == defaultName
            }
            removed = newData.size != list.size
            if (removed) {
                list.clear()
                list.addAll(newData)
                jsonFile.save()
            }
        }

        if (removed) {
            logger.info("Removed DynamicVoiceChannel binding for guild $guildId -> $defaultName")
        }

        return !removed
    }

    fun removeGuild(guildId: Long) {
        jsonGuildManager[guildId].delete()
        logger.info("Removed all DynamicVoiceChannel data for guild $guildId")
    }

    fun getData(categoryId: Long, defaultName: String): DataContainer? {
        synchronized(dataSet) {
            return dataSet.firstOrNull { it.categoryId == categoryId && it.defaultName == defaultName }
        }
    }
}
