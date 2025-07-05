package tw.xinshou.plugin.botinfo.command.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("bot-info")
    val botInfo: LocalTemplate.NDString
)