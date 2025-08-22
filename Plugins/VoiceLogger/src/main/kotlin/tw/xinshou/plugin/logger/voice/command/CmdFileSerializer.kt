package tw.xinshou.plugin.logger.voice.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("voice-logger")
    val voiceLogger: Command,
) {
    @Serializable
    internal data class Command(
        val name: String,
        val description: String,
        val subcommands: SubCommands,
    ) {
        @Serializable
        internal data class SubCommands(
            val setting: LocalTemplate.NameDescriptionString,
        )
    }
}
