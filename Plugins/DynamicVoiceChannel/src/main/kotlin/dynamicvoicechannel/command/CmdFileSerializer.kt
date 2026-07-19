package tw.xinshou.discord.plugin.dynamicvoicechannel.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NameDescriptionString(
    val name: String,
    val description: String,
)

@Serializable
internal data class CmdFileSerializer(
    @SerialName("dynamic-voice-channel")
    val dynamicvc: Command,
) {
    @Serializable
    internal data class Command(
        val name: String,
        val description: String,
        val subcommands: SubCommands,
    ) {
        @Serializable
        internal data class SubCommands(
            val bind: BindCommand,
            val unbind: UnbindCommand,
        ) {
            @Serializable
            internal data class BindCommand(
                val name: String,
                val description: String,
                val options: Options,
            ) {
                @Serializable
                internal data class Options(
                    val channel: NameDescriptionString,
                    @SerialName("format-name-1")
                    val formatName1: NameDescriptionString,
                    @SerialName("format-name-2")
                    val formatName2: NameDescriptionString,
                )
            }

            @Serializable
            internal data class UnbindCommand(
                val name: String,
                val description: String,
                val options: Options,
            ) {
                @Serializable
                internal data class Options(
                    val channel: NameDescriptionString,
                )
            }
        }
    }
}
