package tw.xserver.plugin.botinfo.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xserver.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("bot-info")
    val botInfo: LocalTemplate.NDString
)