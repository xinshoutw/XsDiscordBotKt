package tw.xinshou.plugin.feedbacker.command

import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    val feedbacker: Command1,
) {
    @Serializable
    internal data class Command1(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val member: LocalTemplate.NameDescriptionString
        )
    }
}