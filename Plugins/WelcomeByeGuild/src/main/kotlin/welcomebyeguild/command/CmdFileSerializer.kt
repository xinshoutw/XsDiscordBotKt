package tw.xinshou.discord.plugin.welcomebyeguild.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.discord.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("welcome_channel_bind")
    val welcomeChannelBind: BindCommand,

    @SerialName("welcome_channel_unbind")
    val welcomeChannelUnbind: LocalTemplate.NameDescriptionString,

    @SerialName("bye_channel_bind")
    val byeChannelBind: BindCommand,

    @SerialName("bye_channel_unbind")
    val byeChannelUnbind: LocalTemplate.NameDescriptionString,
) {
    @Serializable
    internal data class BindCommand(
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
