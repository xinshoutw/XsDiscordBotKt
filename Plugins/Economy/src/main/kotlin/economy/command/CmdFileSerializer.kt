package tw.xinshou.discord.plugin.economy.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NameDescriptionString(
    val name: String,
    val description: String,
)

@Serializable
internal data class CmdFileSerializer(
    val balance: CommandMember,
    @SerialName("top-money") val topMoney: NameDescriptionString,
    @SerialName("top-cost") val topCost: NameDescriptionString,
    @SerialName("add-money") val addMoney: CommandMemberValue,
    @SerialName("remove-money") val removeMoney: CommandMemberValue,
    @SerialName("set-money") val setMoney: CommandMemberValue,
    @SerialName("add-cost") val addCost: CommandMemberValue,
    @SerialName("remove-cost") val removeCost: CommandMemberValue,
    @SerialName("set-cost") val setCost: CommandMemberValue
) {
    @Serializable
    internal data class CommandMember(
        val name: String, val description: String, val options: Options
    ) {
        @Serializable
        internal data class Options(val member: NameDescriptionString)
    }

    @Serializable
    internal data class CommandMemberValue(
        val name: String, val description: String, val options: Options
    ) {
        @Serializable
        internal data class Options(val member: NameDescriptionString, val value: NameDescriptionString)
    }
}
