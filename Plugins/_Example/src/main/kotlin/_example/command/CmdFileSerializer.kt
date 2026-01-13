package tw.xinshou.plugin._example.command

import kotlinx.serialization.Serializable
import tw.xinshou.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    val example: Command,
) {
    @Serializable
    internal data class Command(
        val name: String,
        val description: String,
        val options: Options,
    ) {
        @Serializable
        internal data class Options(
            val option1: LocalTemplate.NameDescriptionString,
            val option2: LocalTemplate.NameDescriptionString,
        )
    }
}