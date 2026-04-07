package tw.xinshou.discord.plugin.logger.chat

import core.util.GuildJsonFile
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.plugin.logger.chat.Event.pluginDirectory
import tw.xinshou.discord.plugin.logger.chat.json.DataContainer
import java.io.File
import java.nio.file.Files

internal object JsonManager {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val jsonGuildManager = GuildJsonFile(
        directory = File(pluginDirectory, "setting"),
        serializer = MapSerializer(String.serializer(), DataContainer.serializer()),
        defaultInstance = { mutableMapOf() },
    )

    // listen map to setting
    val dataMap: MutableMap<Long, ChannelData> = HashMap()

    init {
        val settingFolder = File(pluginDirectory, "setting")
        Files.createDirectories(settingFolder.toPath())

        // Note: In v4, GuildJsonFile is lazy-loaded per guild.
        // Initialization of existing data should be done via a startup scan.
        settingFolder.listFiles()?.filter { it.isFile && it.extension == "json" }?.forEach { file ->
            val guildId = file.nameWithoutExtension
            try {
                val jsonFile = jsonGuildManager[guildId]
                jsonFile.data.forEach { (listenChannelId, detectChannelObj) ->
                    val data = DataContainer(
                        allowMode = detectChannelObj.allowMode,
                        allow = detectChannelObj.allow.toMutableSet(),
                        block = detectChannelObj.block.toMutableSet(),
                    )
                    // We can't validate guild channels without JDA instance at init time in v4
                    // Channel validation should be done lazily
                    dataMap[listenChannelId.toLong()] = ChannelData(null, data)
                }
            } catch (e: Exception) {
                logger.error("Error loading setting for guild {}: {}", guildId, e.message)
            }
        }
    }

    fun toggle(guild: Guild, listenChannelId: Long): ChannelData {
        val setting = getChannelData(listenChannelId, guild).toggle()

        val jsonFile = jsonGuildManager[guild.idLong]
        @Suppress("UNCHECKED_CAST")
        (jsonFile.data as MutableMap<String, DataContainer>)
            .getOrPut(listenChannelId.toString()) { DataContainer(false, mutableSetOf(), mutableSetOf()) }
            .allowMode = setting.getChannelMode()
        jsonFile.save()

        return setting
    }

    fun addAllowChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: Set<Long>,
    ): ChannelData {
        val setting = getChannelData(listenChannelId, guild).addAllows(detectedChannelIds)

        val jsonFile = jsonGuildManager[guild.idLong]
        @Suppress("UNCHECKED_CAST")
        (jsonFile.data as MutableMap<String, DataContainer>)
            .getOrPut(listenChannelId.toString()) { DataContainer(false, mutableSetOf(), mutableSetOf()) }
            .allow = setting.getAllow()
        jsonFile.save()

        return setting
    }

    fun addBlockChannels(
        guild: Guild,
        listenChannelId: Long,
        detectedChannelIds: Set<Long>,
    ): ChannelData {
        val setting = getChannelData(listenChannelId, guild).addBlocks(detectedChannelIds)

        val jsonFile = jsonGuildManager[guild.idLong]
        @Suppress("UNCHECKED_CAST")
        (jsonFile.data as MutableMap<String, DataContainer>)
            .getOrPut(listenChannelId.toString()) { DataContainer(false, mutableSetOf(), mutableSetOf()) }
            .block = setting.getBlock()
        jsonFile.save()

        return setting
    }

    fun delete(guildId: Long, channelId: Long) {
        dataMap.remove(channelId)
        jsonGuildManager[guildId].delete()
    }

    private fun getChannelData(listenChannelId: Long, guild: Guild): ChannelData =
        dataMap.computeIfAbsent(listenChannelId) { ChannelData(guild) }
}
