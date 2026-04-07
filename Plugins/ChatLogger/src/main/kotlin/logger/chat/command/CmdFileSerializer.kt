package tw.xinshou.discord.plugin.logger.chat.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
            val setting: NameDescriptionString,
        )
    }
}

@Serializable
internal data class NameDescriptionString(
    val name: String,
    val description: String,
)
