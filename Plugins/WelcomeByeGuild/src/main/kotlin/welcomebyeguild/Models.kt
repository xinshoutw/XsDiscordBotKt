package tw.xinshou.discord.plugin.welcomebyeguild

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.InteractionHook

internal enum class PreviewType {
    JOIN,
    LEAVE,
}

internal data class CreateStep(
    val hook: InteractionHook,
    val guildId: Long,
    val previewUser: User,
    val previewGuildName: String,
    val previewMemberCount: Int,
    var previewType: PreviewType = PreviewType.JOIN,
    val data: GuildSetting,
)

internal data class Template(
    val title: String,
    val description: String,
)

internal data class GuildSetting(
    var channelId: Long = 0L,
    var welcomeTitle: String = "",
    var welcomeDescription: String = "",
    var byeTitle: String = "",
    var byeDescription: String = "",
    var thumbnailUrl: String = "",
    var imageUrl: String = "",
    var welcomeColor: Int = 0x57F287,
    var byeColor: Int = 0xED4245,
)
