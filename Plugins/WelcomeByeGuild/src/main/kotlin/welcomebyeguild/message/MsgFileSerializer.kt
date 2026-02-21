package tw.xinshou.discord.plugin.welcomebyeguild.message

import kotlinx.serialization.Serializable

@Serializable
internal data class MsgFileSerializer(
    val response: Response,
    val setup: Setup,
    val modal: Modal,
    val defaults: Defaults,
) {
    @Serializable
    internal data class Response(
        val guildOnly: String,
        val saveDone: String,
        val invalidColor: String,
    )

    @Serializable
    internal data class Setup(
        val title: String,
        val description: String,
        val selectChannelPlaceholder: String,
        val buttons: Buttons,
        val fields: Fields,
        val outputChannelNotSet: String,
        val thumbnailNotSet: String,
        val imageNotSet: String,
    ) {
        @Serializable
        internal data class Buttons(
            val welcomeText: String,
            val leaveText: String,
            val images: String,
            val colors: String,
            val previewJoin: String,
            val previewLeave: String,
            val save: String,
        )

        @Serializable
        internal data class Fields(
            val outputChannel: String,
            val welcomeMessage: String,
            val leaveMessage: String,
            val thumbnail: String,
            val image: String,
            val welcomeColor: String,
            val leaveColor: String,
        )
    }

    @Serializable
    internal data class Modal(
        val welcomeTitle: String,
        val leaveTitle: String,
        val imagesTitle: String,
        val colorsTitle: String,
        val labels: Labels,
        val placeholders: Placeholders,
    ) {
        @Serializable
        internal data class Labels(
            val title: String,
            val description: String,
            val thumbnailUrl: String,
            val imageUrl: String,
            val welcomeColor: String,
            val leaveColor: String,
        )

        @Serializable
        internal data class Placeholders(
            val thumbnailUrl: String,
            val imageUrl: String,
        )
    }

    @Serializable
    internal data class Defaults(
        val welcomeTitle: String,
        val welcomeDescription: String,
        val leaveTitle: String,
        val leaveDescription: String,
    )
}
