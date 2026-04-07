package tw.xinshou.discord.plugin.ticket.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NameDescriptionString(
    val name: String,
    val description: String,
)

@Serializable
internal data class CmdFileSerializer(
    @SerialName("create_ticket")
    val createTicket: NameDescriptionString,

    @SerialName("add_ticket")
    val addTicket: CommandMessageId,
) {
    @Serializable
    internal data class CommandMessageId(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            @SerialName("message_id")
            val messageId: NameDescriptionString,
        )
    }
}
