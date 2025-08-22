package tw.xinshou.plugin.logger.chat

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.core.base.BotLoader.jdaBot
import tw.xinshou.core.json.JsonFileManager
import tw.xinshou.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.core.json.JsonGuildFileManager
import tw.xinshou.plugin.logger.chat.Event.pluginDirectory
import tw.xinshou.plugin.logger.chat.json.DataContainer
import tw.xinshou.plugin.logger.chat.json.JsonDataClass
import java.io.File
import java.nio.file.Files

internal object JsonManager {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()
    private val jsonGuildManager = JsonGuildFileManager<JsonDataClass>(
        dataDirectory = File(pluginDirectory, "setting"),
        adapter = jsonAdapter,
        defaultInstance = mutableMapOf()
    )

    // listen map to setting
    val dataMap: MutableMap<Long, ChannelData> = HashMap()

    init {
        val settingFolder = File(pluginDirectory, "setting")
        Files.createDirectories(settingFolder.toPath())

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

                val data = DataContainer(
                    allowMode = detectChannelObj.allowMode,
                    allow = detectChannelObj.allow.filter { guild.getGuildChannelById(it) != null }.toMutableSet(),
                    block = detectChannelObj.block.filter { guild.getGuildChannelById(it) != null }.toMutableSet(),
                )
                jsonManager.data[listenChannelId] = data
                dataMap[listenChannelId.toLong()] = ChannelData(guild, data)
            }
            jsonManager.save()
        }
    }

    fun toggle(guild: Guild, listenChannelId: Long): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).toggle()

        // update json file
        val jsonManager = jsonGuildManager.get(guild.idLong)
        jsonManager.data.getOrPut(listenChannelId.toString()) { DataContainer(false, mutableSetOf(), mutableSetOf()) }
            .allowMode = setting.getChannelMode()
        jsonManager.save()

        return setting
    }

    fun addAllowChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: Set<Long>,
    ): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).addAllows(detectedChannelIds)

        // update json file
        val jsonManager = jsonGuildManager.get(guild.idLong)
        jsonManager.data.getOrPut(listenChannelId.toString()) { DataContainer(false, mutableSetOf(), mutableSetOf()) }
            .allow = setting.getAllow()
        jsonManager.save()

        return setting
    }

    fun addBlockChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: Set<Long>,
    ): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).addBlocks(detectedChannelIds)

        // update json file
        val jsonManager = jsonGuildManager.get(guild.idLong)
        jsonManager.data.getOrPut(listenChannelId.toString()) { DataContainer(false, mutableSetOf(), mutableSetOf()) }
            .block = setting.getBlock()
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

