package tw.xinshou.discord.plugin.giveaway.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NameDescriptionString(
    val name: String,
    val description: String,
)

@Serializable
internal data class CmdFileSerializer(
    @SerialName("create_giveaway")
    val createGiveaway: NameDescriptionString,
)
