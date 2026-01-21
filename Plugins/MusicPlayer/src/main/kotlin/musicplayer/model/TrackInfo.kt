package tw.xinshou.discord.plugin.musicplayer.model

/**
 * 音樂來源類型
 */
enum class MusicSource(val displayName: String) {
    YOUTUBE("YouTube"),
    SOUNDCLOUD("SoundCloud"),
    SPOTIFY("Spotify"),
    UNKNOWN("Unknown")
}

/**
 * 增強的音軌資訊類，包含封面圖片和額外資訊
 */
data class EnhancedTrackInfo(
    val title: String,
    val author: String,
    val duration: Long,
    val uri: String,
    val identifier: String,
    val artworkUrl: String? = null,
    val isStream: Boolean = false,
    val position: Long = 0,
    val source: MusicSource = MusicSource.UNKNOWN
)
