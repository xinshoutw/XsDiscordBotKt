package tw.xinshou.plugin.dynamicvoicechannel.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.core.localizations.LocalTemplate

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
                    val channel: LocalTemplate.NameDescriptionString,

                    @SerialName("format-name-1")
                    val formatName1: LocalTemplate.NameDescriptionString,

                    @SerialName("format-name-2")
                    val formatName2: LocalTemplate.NameDescriptionString,
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
                    val channel: LocalTemplate.NameDescriptionString,
                )
            }
        }
    }
}