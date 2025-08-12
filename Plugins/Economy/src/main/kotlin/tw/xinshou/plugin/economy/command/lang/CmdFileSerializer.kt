package tw.xinshou.plugin.economy.command.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    val balance: CommandMember,

    @SerialName("top-money")
    val topMoney: LocalTemplate.NameDescriptionString,

    @SerialName("top-cost")
    val topCost: LocalTemplate.NameDescriptionString,

    @SerialName("add-money")
    val addMoney: CommandMemberValue,

    @SerialName("remove-money")
    val removeMoney: CommandMemberValue,

    @SerialName("set-money")
    val setMoney: CommandMemberValue,

    @SerialName("set-cost")
    val setCost: CommandMemberValue
) {

    @Serializable
    internal data class CommandMember(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val member: LocalTemplate.NameDescriptionString
        )
    }

    @Serializable
    internal data class CommandMemberValue(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val member: LocalTemplate.NameDescriptionString,
            val value: LocalTemplate.NameDescriptionString
        )
    }
}
