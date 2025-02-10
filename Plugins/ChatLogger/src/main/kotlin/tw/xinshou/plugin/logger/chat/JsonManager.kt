package tw.xinshou.plugin.logger.chat

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.BotLoader.jdaBot
import tw.xinshou.loader.json.JsonGuildFileManager
import tw.xinshou.plugin.logger.chat.Event.PLUGIN_DIR_FILE
import tw.xinshou.plugin.logger.chat.json.DataContainer
import tw.xinshou.plugin.logger.chat.json.JsonDataClass
import java.io.File

internal object JsonManager {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val moshi: Moshi = Moshi.Builder().build()
    val jsonAdapter: JsonAdapter<JsonDataClass> = moshi.adapter<JsonDataClass>(JsonDataClass::class.java)
    val jsonGuildManager = JsonGuildFileManager<JsonDataClass>(
        dataDirectory = File(PLUGIN_DIR_FILE, "setting"),
        adapter = jsonAdapter,
        defaultInstance = mutableMapOf()
    )

    // listen map to setting
    val dataMap: MutableMap<Long, ChannelData> = HashMap()

    init {
        val settingFolder = File(PLUGIN_DIR_FILE, "setting")
        if (settingFolder.mkdirs()) {
            logger.info("Default setting folder created")
        }

        jsonGuildManager.mapper.forEach { guildId, jsonManager ->
            // check guild available
            val guild: Guild? = jdaBot.getGuildById(guildId)
            if (guild == null) {
                jsonGuildManager.removeAndSave(guildId)
                return@forEach
            }

            // check json data
            jsonManager.data.forEach { (listenChannelId, detectChannelObj) ->
                if (guild.getGuildChannelById(listenChannelId) == null) {
                    jsonManager.data.remove(listenChannelId)
                    return@forEach
                }

                jsonManager.data[listenChannelId] = DataContainer(
                    allowMode = detectChannelObj.allowMode,
                    allow = detectChannelObj.allow.filter { guild.getGuildChannelById(it) != null }.toMutableList(),
                    block = detectChannelObj.block.filter { guild.getGuildChannelById(it) != null }.toMutableList(),
                )
                dataMap[listenChannelId.toLong()] = ChannelData(guild, this)
            }
            jsonManager.save()
        }
    }

    fun toggle(guild: Guild, listenChannelId: Long): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).toggle()

        // update json file
        val jsonManager = jsonGuildManager.get(guild.idLong)
        jsonManager.data.get(listenChannelId)?.allowMode = setting.getChannelMode()
        jsonManager.save()

        return setting
    }

    fun addAllowChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: List<Long>,
    ): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).addAllows(detectedChannelIds)

        // update json file
        val jsonManager = jsonGuildManager.get(guild.idLong)
        jsonManager.data.get(listenChannelId)?.allow = setting.getAllowArray()
        jsonManager.save()

        return setting
    }

    fun addBlockChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: List<Long>,
    ): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).addBlocks(detectedChannelIds)

        // update json file
        val jsonManager = jsonGuildManager.get(guild.idLong)
        jsonManager.data.get(listenChannelId)?.block = setting.getBlockArray()
        jsonManager.save()

        return setting
    }

    fun delete(guildId: Long, channelId: Long) {
        // remove map
        dataMap.remove(channelId)

        // remove json file
        jsonGuildManager.get(guildId).delete()
    }

    // getOrDefault
    private fun getChannelData(listenChannelId: Long, guild: Guild): ChannelData =
        dataMap.computeIfAbsent(listenChannelId) { ChannelData(guild) }
}