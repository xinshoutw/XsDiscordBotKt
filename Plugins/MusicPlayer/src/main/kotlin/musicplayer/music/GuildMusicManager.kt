package tw.xinshou.discord.plugin.musicplayer.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Member
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.plugin.musicplayer.Event
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 公會音樂管理器，負責管理特定公會的音樂播放功能
 *
 * 此類為每個公會維護獨立的音頻播放器、調度器和發送處理器
 */
class GuildMusicManager(
    private val audioPlayer: AudioPlayer,
    private val guildId: Long,
    private val idleCheckScheduler: ScheduledExecutorService? = null
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val IDLE_TIMEOUT_MINUTES = 10L
    }

    private val totalTracksPlayed = AtomicLong(0)
    private val totalPlaytimeMs = AtomicLong(0)
    private val lastActivityTime = AtomicLong(System.currentTimeMillis())
    private val idleCheckTask = AtomicReference<ScheduledFuture<*>?>(null)
    val sendHandler: AudioPlayerSendHandler
    val createdAt: Instant = Instant.now()

    // 新的 History_index 管理器 - 替代原有的 TrackScheduler 和 TrackContextManager
    private val historyIndexManager: HistoryIndexManager = HistoryIndexManager(audioPlayer)

    // 向後兼容的 scheduler 屬性（委派給 historyIndexManager）
    val scheduler: TrackSchedulerCompat = TrackSchedulerCompat(historyIndexManager)

    init {
        // 配置音頻播放器
        audioPlayer.addListener(scheduler)

        // 從配置文件獲取預設音量，如果獲取失敗則使用 15
        val defaultVolume = try {
            Event.config.player.defaultVolume
        } catch (e: Exception) {
            logger.warn("Failed to get default volume from config, using 15", e)
            15
        }

        // 設置音量，確保在有效範圍內
        audioPlayer.volume = defaultVolume.coerceIn(0, 100)

        // 創建優化的音頻發送處理器
        sendHandler = AudioPlayerSendHandler(audioPlayer)

        // 啟動閒置檢查
        startIdleCheck()
    }

    /**
     * 啟動閒置檢查
     */
    private fun startIdleCheck() {
        idleCheckScheduler?.let { exec ->
            val task = exec.scheduleAtFixedRate({
                checkIdleStatus()
            }, IDLE_TIMEOUT_MINUTES, 1, TimeUnit.MINUTES)

            idleCheckTask.set(task)
        }
    }

    /**
     * 檢查閒置狀態
     */
    private fun checkIdleStatus() {
        try {
            val now = System.currentTimeMillis()
            val timeSinceLastActivity = now - lastActivityTime.get()
            val idleTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeSinceLastActivity)

            if (idleTimeMinutes < IDLE_TIMEOUT_MINUTES) {
                return
            }

            // 檢查是否正在播放
            if (audioPlayer.playingTrack == null && scheduler.getQueueSize() == 0) {
                onIdleTimeout()
            }
        } catch (e: Exception) {
            logger.error("Error during idle check for guild: $guildId", e)
        }
    }

    /**
     * 閒置超時處理
     */
    private fun onIdleTimeout() {
        try {
            audioPlayer.stopTrack()
            scheduler.clearQueue()
            logger.info("Auto-disconnected from guild $guildId due to inactivity")
        } catch (e: Exception) {
            logger.error("Error during idle timeout handling for guild: $guildId", e)
        }
    }

    /**
     * 更新最後活動時間
     */
    fun updateActivity() {
        lastActivityTime.set(System.currentTimeMillis())
    }

    /**
     * 播放音軌
     * @param track 要播放的音軌
     * @param requester 點播此歌曲的成員
     * @return true if the track was successfully queued, false if it was already playing or an error occurred
     */
    fun playTrack(track: AudioTrack, requester: Member): Boolean {
        return try {
            updateActivity()

            // 確保音軌資料完整
            if (track.info?.title.isNullOrBlank()) {
                logger.warn("Track has invalid info, skipping: ${track.identifier}")
                return false
            }

            // 儲存點播者資訊到音軌的 UserData 中
            track.userData = mapOf(
                "requesterId" to requester.id,
                "requesterName" to requester.effectiveName,
                "requesterUsername" to requester.user.name,
                "requesterAvatarUrl" to requester.effectiveAvatarUrl
            )

            // 使用新的 HistoryIndexManager 來處理音軌播放
            val success = historyIndexManager.addTrack(track, requester)

            if (success) {
                totalTracksPlayed.incrementAndGet()

                // 如果音頻播放器處於不正常狀態，嘗試重置
                if (audioPlayer.isPaused && audioPlayer.playingTrack == null) {
                    audioPlayer.isPaused = false
                }
            } else {
                logger.warn("Failed to queue track: ${track.info.title}")
            }

            success
        } catch (e: Exception) {
            logger.error("Error occurred while playing track: ${track.info?.title}", e)
            false
        }
    }

    /**
     * 暫停播放
     * @return true if successfully paused, false if no track is playing or already paused
     */
    fun pause(): Boolean {
        return try {
            if (audioPlayer.playingTrack != null && !audioPlayer.isPaused) {
                audioPlayer.isPaused = true
                updateActivity()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Error occurred while pausing for guild: $guildId", e)
            false
        }
    }

    /**
     * 恢復播放
     * @return true if successfully resumed, false if no track is playing or already playing
     */
    fun resume(): Boolean {
        return try {
            if (audioPlayer.playingTrack != null && audioPlayer.isPaused) {
                audioPlayer.isPaused = false
                updateActivity()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Error occurred while resuming for guild: $guildId", e)
            false
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        audioPlayer.stopTrack()
        scheduler.clearQueue()
        updateActivity()
    }

    /**
     * 跳過當前音軌
     * 修復：直接使用 HistoryIndexManager.skip() 避免雙重跳過問題
     */
    fun skip(): Boolean {
        return try {
            if (audioPlayer.playingTrack != null) {
                updateActivity()

                // 確保播放器沒有暫停
                if (audioPlayer.isPaused) {
                    audioPlayer.isPaused = false
                }

                // 直接使用 HistoryIndexManager.skip() 避免雙重跳過
                // 這會正確地增加 History_index 並開始下一首歌曲
                historyIndexManager.skip()
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Error occurred while skipping for guild: $guildId", e)
            false
        }
    }

    /**
     * 設定音量
     * @param volume 音量值，範圍 0-100
     * @return true 如果音量設置成功，false 如果音量值不在範圍內
     */
    fun setVolume(volume: Int): Boolean {
        return try {
            if (volume !in 0..100) {
                logger.warn("Invalid volume value: $volume, must be between 0-100")
                return false
            }

            audioPlayer.volume = volume
            updateActivity()
            true
        } catch (e: Exception) {
            logger.error("Error occurred while setting volume to $volume for guild: $guildId", e)
            false
        }
    }

    /**
     * 獲取當前音量
     */
    fun getVolume(): Int = audioPlayer.volume

    /**
     * 檢查是否正在播放
     */
    fun isPlaying(): Boolean = audioPlayer.playingTrack != null && !audioPlayer.isPaused

    /**
     * 檢查是否已暫停
     */
    fun isPaused(): Boolean = audioPlayer.isPaused

    /**
     * 獲取當前播放的音軌
     */
    fun getCurrentTrack(): AudioTrack? = audioPlayer.playingTrack

    /**
     * 獲取 History_index 管理器
     */
    fun getHistoryIndexManager(): HistoryIndexManager = historyIndexManager

    /**
     * 獲取播放統計數據
     */
    fun getStats(): MusicStats {
        val currentTrack = audioPlayer.playingTrack
        val currentPlaytime = currentTrack?.position ?: 0

        return MusicStats(
            guildId = guildId,
            totalTracksPlayed = totalTracksPlayed.get(),
            totalPlaytimeMs = totalPlaytimeMs.get() + currentPlaytime,
            queueSize = scheduler.getQueueSize(),
            isPlaying = isPlaying(),
            isPaused = isPaused(),
            volume = audioPlayer.volume,
            createdAt = createdAt,
            lastActivity = Instant.ofEpochMilli(lastActivityTime.get())
        )
    }

    /**
     * 清理資源
     */
    fun cleanup() {
        try {
            // 取消閒置檢查任務
            idleCheckTask.get()?.cancel(true)

            // 停止播放器
            audioPlayer.stopTrack()
            audioPlayer.destroy()

            // 清理調度器和 History_index 管理器
            scheduler.clearQueue()
            scheduler.clearHistory()

            // 清理發送處理器
            sendHandler.close()
        } catch (e: Exception) {
            logger.error("Error during cleanup for guild: $guildId", e)
        }
    }

    /**
     * 音樂統計數據類
     */
    data class MusicStats(
        val guildId: Long,
        val totalTracksPlayed: Long,
        val totalPlaytimeMs: Long,
        val queueSize: Int,
        val isPlaying: Boolean,
        val isPaused: Boolean,
        val volume: Int,
        val createdAt: Instant,
        val lastActivity: Instant
    )
}
