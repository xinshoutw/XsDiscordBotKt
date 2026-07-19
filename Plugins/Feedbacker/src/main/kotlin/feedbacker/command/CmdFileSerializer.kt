package tw.xinshou.discord.plugin.feedbacker.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CmdFileSerializer(
    val feedbacker: Command1,
) {
    @Serializable
    internal data class Command1(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val member: NameDescriptionString,

            @SerialName("submit-channel")
            val submitChannel: NameDescriptionString,
        )
    }
}

@Serializable
internal data class NameDescriptionString(
    val name: String,
    val description: String,
)
