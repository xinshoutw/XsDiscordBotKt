package tw.xinshou.plugin.logger.chat.command.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("chat-logger")
    val chatLogger: Command,
) {
    @Serializable
    internal data class Command(
        val name: String,
        val description: String,
        val subcommands: SubCommands,
    ) {
        @Serializable
        internal data class SubCommands(
            val setting: LocalTemplate.NDString,
        )
    }
}
