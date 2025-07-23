package tw.xinshou.plugin.musicplayer.util

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.managers.AudioManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.builtin.placeholder.Substitutor
import tw.xinshou.plugin.musicplayer.model.EnhancedTrackInfo
import tw.xinshou.plugin.musicplayer.model.MusicSource
import tw.xinshou.plugin.musicplayer.music.GuildMusicManager

/**
 * 音樂播放器工具類，包含常用的驗證和操作方法
 */
internal object MusicPlayerUtils {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // 音樂播放器進度條相關常數
    private const val PROGRESS_BAR_LENGTH = 15

    // 音樂播放器 Emoji 常數
    private const val EMOJI_PROGRESS_WHITE_START = "<:music_player__line_white_start:1395405244929802240>"
    private const val EMOJI_PROGRESS_WHITE_FULL = "<:music_player__line_white_full:1395405230618972160>"
    private const val EMOJI_PROGRESS_WHITE_END = "<:music_player__line_white_end:1395405218723664012>"
    private const val EMOJI_PROGRESS_BLACK = "<:music_player__line_black:1395405206736470118>"
    private const val EMOJI_PROGRESS_BLACK_END = "<:music_player__line_black_end:1395405187069378560>"
    private const val EMOJI_MEDIA_PLAY = "<:music_player__media_play:1395593840500408421>"
    private const val EMOJI_MEDIA_PAUSE = "<:music_player__media_pause:1395593832384565248>"
    private val BAR_ARRAY = arrayOf(
        EMOJI_PROGRESS_WHITE_START + EMOJI_PROGRESS_BLACK.repeat(PROGRESS_BAR_LENGTH - 2) + EMOJI_PROGRESS_BLACK_END,
        *((1..PROGRESS_BAR_LENGTH - 2).map {
            EMOJI_PROGRESS_WHITE_START +
                    EMOJI_PROGRESS_WHITE_FULL.repeat(it) +
                    EMOJI_PROGRESS_BLACK.repeat(PROGRESS_BAR_LENGTH - 2 - it) +
                    EMOJI_PROGRESS_BLACK_END
        }.toTypedArray()),
        EMOJI_PROGRESS_WHITE_START + EMOJI_PROGRESS_WHITE_FULL.repeat(PROGRESS_BAR_LENGTH - 2) + EMOJI_PROGRESS_WHITE_END,
    )

    /**
     * 檢查用戶是否在語音頻道中
     */
    fun isUserInVoiceChannel(member: Member?): Boolean {
        return member?.voiceState?.channel != null
    }

    /**
     * 獲取用戶的語音頻道
     */
    fun getUserVoiceChannel(member: Member?): VoiceChannel? {
        return member?.voiceState?.channel as? VoiceChannel
    }

    /**
     * 連接到語音頻道
     */
    internal fun connectToVoiceChannel(audioManager: AudioManager, voiceChannel: VoiceChannel): Boolean {
        return try {
            audioManager.openAudioConnection(voiceChannel)
            true
        } catch (e: Exception) {
            logger.error("Failed to connect to voice channel: ${voiceChannel.name}", e)
            false
        }
    }

    /**
     * 驗證用戶語音狀態和權限
     */
    fun validateVoiceState(event: SlashCommandInteractionEvent): ValidationResult {
        val member = event.member ?: return ValidationResult.Error("無法獲取用戶資訊")
        val guild = event.guild ?: return ValidationResult.Error("無法獲取伺服器資訊")

        if (!isUserInVoiceChannel(member)) {
            return ValidationResult.Error("您必須先加入語音頻道才能使用此功能！")
        }

        val voiceChannel = getUserVoiceChannel(member)
            ?: return ValidationResult.Error("無法找到您的語音頻道！")

        return ValidationResult.Success(guild, member, voiceChannel)
    }

    /**
     * 格式化時間顯示
     */
    internal fun formatTime(milliseconds: Long): String {
        if (milliseconds == Long.MAX_VALUE) return "∞ (直播)"

        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            else -> String.format("%02d:%02d", minutes, seconds % 60)
        }
    }

    /**
     * 生成音樂播放器進度條＝＝
     */
    private fun generateProgressBar(
        currentPosition: Long,
        totalDuration: Long,
        isPlaying: Boolean,
    ): String {
        // 如果是直播，返回簡單的播放/暫停狀態
        if (totalDuration == Long.MAX_VALUE || totalDuration <= 0) {
            val playPauseEmoji = if (isPlaying) EMOJI_MEDIA_PLAY else EMOJI_MEDIA_PAUSE
            return "$playPauseEmoji 直播中"
        }

        val progress = (currentPosition.toDouble() / totalDuration.toDouble()).coerceIn(0.1, 1.0)
        val playPauseEmoji = if (isPlaying) EMOJI_MEDIA_PLAY else EMOJI_MEDIA_PAUSE

        return "$playPauseEmoji " +
                formatTime(currentPosition) +
                BAR_ARRAY[(progress * PROGRESS_BAR_LENGTH).toInt() - 1] +
                formatTime(totalDuration)
    }

    /**
     * 獲取音源名稱
     */
    internal fun getSourceName(uri: String): String {
        return when {
            uri.contains("youtube.com") || uri.contains("youtu.be") -> "YouTube"
            uri.contains("soundcloud.com") -> "SoundCloud"
            uri.contains("spotify.com") -> "Spotify"
            else -> "其他來源"
        }
    }

    /**
     * 處理智能查詢
     */
    internal fun processSearchQuery(query: String): String {
        return when {
            query.startsWith("http") -> query // 直接URL
            query.contains("youtube.com") || query.contains("youtu.be") -> query
            query.contains("soundcloud.com") -> query
            query.contains("spotify.com") -> query
            else -> "ytsearch:$query" // 默認YouTube搜索
        }
    }

    /**
     * 從YouTube URL提取視頻ID
     */
    fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com(?:\\.\\w{2})?/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]+)",
            "youtube\\.com(?:\\.\\w{2})?/embed/([a-zA-Z0-9_-]+)",
            "youtube\\.com(?:\\.\\w{2})?/v/([a-zA-Z0-9_-]+)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(url)
            if (matchResult != null) {
                return matchResult.groupValues[1]
            }
        }
        return null
    }

    /**
     * 從AudioTrack提取增強資訊
     */
    internal fun extractEnhancedInfo(track: AudioTrack): EnhancedTrackInfo {
        val info = track.info
        var artworkUrl: String? = null
        var source: MusicSource = MusicSource.UNKNOWN

        // 嘗試從不同源提取封面圖片和來源資訊
        when {
            info.uri.contains("youtube.com") || info.uri.contains("youtu.be") -> {
                val videoId = extractYouTubeVideoId(info.uri) ?: info.identifier
                artworkUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                source = MusicSource.YOUTUBE
            }

            info.uri.contains("soundcloud.com") -> {
                artworkUrl = track.getUserData(String::class.java)
                source = MusicSource.SOUNDCLOUD
            }

            info.uri.contains("spotify.com") -> {
                artworkUrl = track.getUserData(String::class.java)
                source = MusicSource.SPOTIFY
            }
        }

        return EnhancedTrackInfo(
            title = info.title,
            author = info.author,
            duration = info.length,
            uri = info.uri,
            identifier = info.identifier,
            artworkUrl = artworkUrl,
            isStream = info.isStream,
            position = track.position,
            source = source
        )
    }

    internal fun getSubstitutor(
        baseSubstitutor: Substitutor,
        track: AudioTrack,
        musicManager: GuildMusicManager
    ): Substitutor {
        return baseSubstitutor.apply {
            extractEnhancedInfo(track).let { info ->
                put("music_player@song_author_name", info.author)

                put("music_player@song_name", info.title)
                put("music_player@song_url", info.uri)

                put("music_player@song_thumbnail_url", info.artworkUrl ?: "https://i.meee.com.tw/GHexAW6.png")

                put("music_player@track_duration", formatTime(info.duration))

                // 生成進度條（包含播放/暫停狀態和進度指示器）
                put(
                    "music_player@song_duration", generateProgressBar(
                        currentPosition = info.position,
                        totalDuration = info.duration,
                        isPlaying = musicManager.isPlaying()
                    )
                )
            }

            // 提取點播者資訊
            try {
                val requesterData = track.userData as? Map<*, *>
                if (requesterData != null) {
                    put(
                        "music_player@song_requester_username",
                        requesterData["requesterUsername"]?.toString() ?: "未知用戶"
                    )
                    put("music_player@song_requester_name", requesterData["requesterName"]?.toString() ?: "未知用戶")
                    put("music_player@song_requester_avatar_url", requesterData["requesterAvatarUrl"]?.toString() ?: "")
                    put("music_player@song_requester_id", requesterData["requesterId"]?.toString() ?: "")
                } else {
                    // 如果沒有點播者資訊，使用預設值
                    put("music_player@song_requester_username", "未知用戶")
                    put("music_player@song_requester_name", "未知用戶")
                    put("music_player@song_requester_avatar_url", "")
                    put("music_player@song_requester_id", "")
                }
            } catch (e: Exception) {
                // 如果提取失敗，使用預設值
                put("music_player@song_requester_username", "未知用戶")
                put("music_player@song_requester_name", "未知用戶")
                put("music_player@song_requester_avatar_url", "")
                put("music_player@song_requester_id", "")
            }

            if (musicManager.isPlaying()) {
                put("music_player@player_current_color", "0x68c06e")
            } else {
                put("music_player@player_current_color", "0xba9c55")
            }

            if (musicManager.scheduler.isSequentialPlayback()) {
                put(
                    "music_player@player_currnet_loop_mode_emoji",
                    "<:music_player__media_ordered:1396616068679598120>"
                )
            } else if (musicManager.scheduler.isShufflePlayback()) {
                put(
                    "music_player@player_currnet_loop_mode_emoji",
                    "<:music_player__media_shuffle:1395593799677382766>"
                )
            } else if (musicManager.scheduler.isSingleTrackLoop()) {
                put(
                    "music_player@player_currnet_loop_mode_emoji",
                    "<:music_player__media_repeat_once:1396395280236286063>"
                )
            } else if (musicManager.scheduler.isQueueLoop()) {
                put(
                    "music_player@player_currnet_loop_mode_emoji",
                    "<:music_player__media_loop:1395593790835785928>"
                )
            }
        }
    }

    /**
     * 驗證結果封裝類
     */
    sealed class ValidationResult {
        data class Success(val guild: Guild, val member: Member, val voiceChannel: VoiceChannel) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}