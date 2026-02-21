package tw.xinshou.discord.plugin.welcomebyeguild.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.discord.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("welcome_channel_bind")
    val welcomeChannelBind: ChannelCommand,

    @SerialName("welcome_channel_unbind")
    val welcomeChannelUnbind: ChannelCommand,

    @SerialName("bye_channel_bind")
    val byeChannelBind: ChannelCommand,

    @SerialName("bye_channel_unbind")
    val byeChannelUnbind: ChannelCommand,
) {
    @Serializable
    internal data class ChannelCommand(
        val name: String,
        val description: String,
        val options: Options,
    ) {
        @Serializable
        internal data class Options(
            val channel: LocalTemplate.NameDescriptionString,
        )
    }
}
