package tw.xinshou.plugin.simplecommand.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("ctcb-none-card")
    val ctcbNoneCard: LocalTemplate.NameDescriptionString,

    @SerialName("ctcb-remit")
    val ctcbRemit: LocalTemplate.NameDescriptionString,

    @SerialName("chpytwtp-remit")
    val chpytwtpRemit: LocalTemplate.NameDescriptionString,
)