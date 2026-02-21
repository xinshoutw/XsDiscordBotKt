package tw.xinshou.discord.plugin.welcomebyeguild

internal data class GuildSetting(
    var welcomeChannelId: Long = 0L,
    var byeChannelId: Long = 0L,
)
