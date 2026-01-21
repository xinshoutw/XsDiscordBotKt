package tw.xinshou.discord.plugin.botinfo.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.discord.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("bot-info")
    val botInfo: LocalTemplate.NameDescriptionString
)