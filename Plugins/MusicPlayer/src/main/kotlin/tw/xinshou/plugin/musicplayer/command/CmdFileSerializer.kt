package tw.xinshou.plugin.musicplayer.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    val join: LocalTemplate.NameDescriptionString,
    val play: CommandPlay,
    val pause: LocalTemplate.NameDescriptionString,
    val resume: LocalTemplate.NameDescriptionString,
    val stop: LocalTemplate.NameDescriptionString,
    val disconnect: LocalTemplate.NameDescriptionString,
    val skip: CommandSkip,
    val volume: CommandVolume,
    val queue: LocalTemplate.NameDescriptionString,

    @SerialName("now_playing")
    val nowPlaying: LocalTemplate.NameDescriptionString,
) {
    @Serializable
    internal data class CommandPlay(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val query: LocalTemplate.NameDescriptionString
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
            val count: LocalTemplate.NameDescriptionString
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
            val level: LocalTemplate.NameDescriptionString
        )
    }
}