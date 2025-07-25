package tw.xinshou.plugin.simplecommand.command.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("ctcb-none-card")
    val ctcbNoneCard: LocalTemplate.NDString,

    @SerialName("ctcb-remit")
    val ctcbRemit: LocalTemplate.NDString,

    @SerialName("chpytwtp-remit")
    val chpytwtpRemit: LocalTemplate.NDString,
)
