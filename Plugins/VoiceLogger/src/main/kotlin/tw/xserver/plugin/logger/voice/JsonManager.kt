package tw.xserver.plugin.logger.voice

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.base.BotLoader.jdaBot
import tw.xserver.loader.json.JsonObjFileManager
import tw.xserver.plugin.logger.voice.Event.PLUGIN_DIR_FILE
import java.io.File

internal object JsonManager {
    private val fileManagers: MutableMap<Long, JsonObjFileManager> = HashMap()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // listen map to setting
    internal val dataMap: MutableMap<Long, ChannelData> = HashMap()

    init {
        val settingFolder = File(PLUGIN_DIR_FILE, "setting")
        if (settingFolder.mkdirs()) {
            logger.info("Default setting folder created")
        }

        // delete unable access guild json files
        settingFolder.listFiles()?.filter { it.isFile && it.extension == "json" }?.forEach fileLoop@{ file: File ->
            val guildId = file.nameWithoutExtension.toLong()
            val guild: Guild? = jdaBot.getGuildById(guildId)
            if (guild == null) {
                file.delete()
                return@fileLoop
            }

            val fileManager = JsonObjFileManager(file)
            this.fileManagers[guildId] = fileManager

            // put data from json files to channelSettings map
            fileManager.get().entrySet().forEach channelLoop@{ (listenChannelId, detectChannelObj) ->
                if (guild.getGuildChannelById(listenChannelId) == null) {
                    fileManager.remove(listenChannelId)
                    return@channelLoop
                }

                if (detectChannelObj !is JsonObject) return@channelLoop

                fileManager.getAsJsonObject(listenChannelId).apply {
                    add("allow", JsonArray().apply {
                        detectChannelObj["allow"].asJsonArray
                            .filter { guild.getGuildChannelById(it.asLong) != null }
                            .map { it.asLong }
                            .forEach { add(it) }
                    })
                    add("block", JsonArray().apply {
                        detectChannelObj["block"].asJsonArray
                            .filter { guild.getGuildChannelById(it.asLong) != null }
                            .map { it.asLong }
                            .forEach { add(it) }
                    })

                    dataMap[listenChannelId.toLong()] = ChannelData(guild, this)
                }
            }

            fileManager.save()
        }
    }

    private fun getOrPut(guild: Guild, listenChannelId: Long): Pair<JsonObjFileManager, JsonObject> {
        val fileManager = fileManagers
            .getOrPut(guild.idLong) { JsonObjFileManager(File(PLUGIN_DIR_FILE, "setting/${guild.id}.json")) }
        val obj: JsonObject =
            fileManager.computeIfAbsent(listenChannelId.toString(), ChannelData(guild).getJsonObject()).asJsonObject

        return Pair(fileManager, obj)
    }

    internal fun toggle(guild: Guild, listenChannelId: Long): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).toggle()

        // update json file
        val (fileManager, obj) = getOrPut(guild, listenChannelId)
        obj.addProperty("allow_mode", setting.getChannelMode())
        fileManager.save()

        return setting
    }

    internal fun addAllowChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: List<Long>,
    ): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).addAllows(detectedChannelIds)

        // update json file
        val (fileManager, obj) = getOrPut(guild, listenChannelId)
        obj.add("allow", setting.getAllowArray())
        fileManager.save()

        return setting
    }

    internal fun addBlockChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: List<Long>,
    ): ChannelData {
        // update map
        val setting = getChannelData(listenChannelId, guild).addBlocks(detectedChannelIds)

        // update json file
        val (fileManager, obj) = getOrPut(guild, listenChannelId)
        obj.add("block", setting.getBlockArray())
        fileManager.save()

        return setting
    }

    fun delete(guildId: Long, channelId: Long) {
        // remove map
        dataMap.remove(channelId)

        // remove json file
        fileManagers[guildId]?.remove(channelId.toString())?.save()
    }

    // getOrDefault
    private fun getChannelData(listenChannelId: Long, guild: Guild): ChannelData {
        return dataMap.computeIfAbsent(listenChannelId) { ChannelData(guild) }
    }
}