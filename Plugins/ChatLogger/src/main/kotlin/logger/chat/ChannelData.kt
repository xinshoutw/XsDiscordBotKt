package tw.xinshou.discord.plugin.logger.chat

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import tw.xinshou.discord.plugin.logger.chat.json.DataContainer

internal class ChannelData(
    private val guild: Guild,
    initData: DataContainer? = null
) {
    private var channelMode: ChannelMode = ChannelMode.Allow
    private val allow: MutableSet<Long> = mutableSetOf()
    private val block: MutableSet<Long> = mutableSetOf()

    init {
        initData?.let {
            when (it.allowMode) {
                true -> ChannelMode.Allow
                false -> ChannelMode.Block
            }.also { mode -> channelMode = mode }

            addAll(it)
        }
    }

    fun getChannelMode(): Boolean = channelMode == ChannelMode.Allow
    fun getAllow() = allow
    fun getBlock() = block

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

    fun addAllows(detectedChannelIds: Set<Long>): ChannelData {
        detectedChannelIds.forEach(allow::add)
        return this
    }

    fun addBlocks(detectedChannelIds: Set<Long>): ChannelData {
        detectedChannelIds.forEach(block::add)
        return this
    }

    fun addAll(obj: DataContainer): ChannelData {
        addAllows(obj.allow)
        addBlocks(obj.block)
        return this
    }

    enum class ChannelMode {
        Allow,
        Block
    }
}