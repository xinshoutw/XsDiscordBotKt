package tw.xinshou.plugin.basiccalculator.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("basic-calculate")
    val basicCalculate: Command,
) {
    @Serializable
    internal data class Command(
        val name: String,
        val description: String,
        val options: Options,
    ) {
        @Serializable
        internal data class Options(
            val formula: LocalTemplate.NameDescriptionString
        )
    }
}