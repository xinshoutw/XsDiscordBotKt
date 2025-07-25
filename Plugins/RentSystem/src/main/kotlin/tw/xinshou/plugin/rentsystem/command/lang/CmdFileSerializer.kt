package tw.xinshou.plugin.rentsystem.command.lang

import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    val meter: Command1,
) {
    @Serializable
    internal data class Command1(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val public: LocalTemplate.NDString,
            val roomA: LocalTemplate.NDString,
            val roomB: LocalTemplate.NDString,
            val roomC: LocalTemplate.NDString,
        )
    }
}
