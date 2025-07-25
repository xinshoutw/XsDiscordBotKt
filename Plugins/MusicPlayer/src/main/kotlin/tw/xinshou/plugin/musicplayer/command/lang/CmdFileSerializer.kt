package tw.xinshou.plugin.musicplayer.command.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    val join: LocalTemplate.NDString,
    val play: CommandPlay,
    val pause: LocalTemplate.NDString,
    val resume: LocalTemplate.NDString,
    val stop: LocalTemplate.NDString,
    val disconnect: LocalTemplate.NDString,
    val skip: CommandSkip,
    val volume: CommandVolume,
    val queue: LocalTemplate.NDString,

    @SerialName("now_playing")
    val nowPlaying: LocalTemplate.NDString,
) {
    @Serializable
    internal data class CommandPlay(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val query: LocalTemplate.NDString
        )
    }

    @Serializable
    internal data class CommandSkip(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val count: LocalTemplate.NDString
        )
    }

    @Serializable
    internal data class CommandVolume(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val level: LocalTemplate.NDString
        )
    }
}