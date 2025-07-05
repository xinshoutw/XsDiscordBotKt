package tw.xinshou.plugin.ticket.command.lang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("create_ticket")
    val createTicket: LocalTemplate.NDString,

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
            val messageId: LocalTemplate.NDString,
        )
    }
}
