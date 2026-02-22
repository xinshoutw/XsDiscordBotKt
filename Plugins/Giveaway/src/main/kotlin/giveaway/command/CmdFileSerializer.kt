package tw.xinshou.discord.plugin.giveaway.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.discord.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("create_giveaway")
    val createGiveaway: LocalTemplate.NameDescriptionString,
)
