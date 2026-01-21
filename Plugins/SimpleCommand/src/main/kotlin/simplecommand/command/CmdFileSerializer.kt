package tw.xinshou.discord.plugin.simplecommand.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.discord.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("cub-none-card") // 國泰
    val cubNoneCard: LocalTemplate.NameDescriptionString,

    @SerialName("tsib-none-card") // 台新
    val tsibNoneCard: LocalTemplate.NameDescriptionString,

    @SerialName("ctcb-none-card") // 中信
    val ctcbNoneCard: LocalTemplate.NameDescriptionString,

    @SerialName("ctcb-remit") // 中信
    val ctcbRemit: LocalTemplate.NameDescriptionString,

    @SerialName("chpytwtp-remit") // 郵局
    val chpytwtpRemit: LocalTemplate.NameDescriptionString,
)