package tw.xinshou.plugin.logger.chat

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

internal class ChannelData(
    private val guild: Guild,
    initData: JsonObject? = null
) {
    private var channelMode: ChannelMode = ChannelMode.Allow
    private val allow: MutableSet<Long> = mutableSetOf()
    private val block: MutableSet<Long> = mutableSetOf()

    fun getJsonObject(): JsonObject = JsonObject().apply {
        addProperty("allow_mode", getChannelMode())
        add("allow", getAllowArray())
        add("block", getBlockArray())
    }

    fun getChannelMode(): Boolean = channelMode == ChannelMode.Allow
    fun getAllowArray(): JsonArray = JsonArray().apply { allow.forEach(::add) }
    fun getBlockArray(): JsonArray = JsonArray().apply { block.forEach(::add) }

    init {
        initData?.let {
            when (it["allow_mode"].asBoolean) {
                true -> ChannelMode.Allow
                false -> ChannelMode.Block
            }.also { mode -> channelMode = mode }

            addAll(it)
        }
    }

    fun getCurrentDetectChannels(): List<GuildChannel> = when (channelMode) {
        ChannelMode.Allow -> {
            allow.mapNotNull { guild.getGuildChannelById(it) }
                .flatMap { channel ->
                    if (channel is Category) channel.channels else listOf(channel)
                }
        }

        ChannelMode.Block -> {
            block.mapNotNull { guild.getGuildChannelById(it) }
                .flatMap { channel ->
                    if (channel is Category) channel.channels else listOf(channel)
                }
                .let { ignoreChannels -> guild.channels.filter { it !in ignoreChannels } }
        }
    }


    fun toggle(): ChannelData {
        channelMode = if (channelMode == ChannelMode.Allow) {
            ChannelMode.Block
        } else {
            ChannelMode.Allow
        }
        return this
    }

    fun addAllows(detectedChannelIds: List<Long>): ChannelData {
        detectedChannelIds.forEach(allow::add)
        return this
    }

    fun addBlocks(detectedChannelIds: List<Long>): ChannelData {
        detectedChannelIds.forEach(block::add)
        return this
    }

    fun addAll(obj: JsonObject): ChannelData {
        addAllows(obj["allow"].asJsonArray.map { it.asLong })
        addBlocks(obj["block"].asJsonArray.map { it.asLong })
        return this
    }

    enum class ChannelMode {
        Allow,
        Block
    }
}