package tw.xinshou.discord.plugin.musicplayer.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NameDescriptionString(
    val name: String,
    val description: String,
)

@Serializable
internal data class CmdFileSerializer(
    val join: NameDescriptionString,
    val play: CommandPlay,
    val pause: NameDescriptionString,
    val resume: NameDescriptionString,
    val stop: NameDescriptionString,
    val disconnect: NameDescriptionString,
    val skip: CommandSkip,
    val volume: CommandVolume,
    val queue: NameDescriptionString,
    val shuffle: NameDescriptionString,
    @SerialName("now_playing")
    val nowPlaying: NameDescriptionString,
) {
    @Serializable
    internal data class CommandPlay(
        val name: String, val description: String, val options: Options
    ) {
        @Serializable
        internal data class Options(val query: NameDescriptionString)
    }

    @Serializable
    internal data class CommandSkip(
        val name: String, val description: String, val options: Options
    ) {
        @Serializable
        internal data class Options(val count: NameDescriptionString)
    }

    @Serializable
    internal data class CommandVolume(
        val name: String, val description: String, val options: Options
    ) {
        @Serializable
        internal data class Options(val level: NameDescriptionString)
    }
}
