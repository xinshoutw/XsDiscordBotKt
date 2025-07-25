package tw.xinshou.plugin.musicplayer.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 向後兼容的 TrackScheduler 包裝器
 *
 * 提供與原有 TrackScheduler 相同的接口，但內部使用新的 HistoryIndexManager
 * 實現用戶期望的 History_index 行為
 */
class TrackSchedulerCompat(
    private val historyIndexManager: HistoryIndexManager
) : AudioEventAdapter() {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    // Playback mode state
    private var sequentialPlayback: Boolean = true
    private var singleTrackLoop: Boolean = false

    /**
     * 音軌結束事件處理
     * 根據播放模式決定下一步行為
     */
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        try {
            // 只在正常結束時處理播放邏輯
            if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.STOPPED || endReason == AudioTrackEndReason.LOAD_FAILED) {
                when {
                    singleTrackLoop -> {
                        // 單曲循環：重新播放當前歌曲
                        val currentTrack = historyIndexManager.currentTrack
                        if (currentTrack != null) {
                            player.startTrack(currentTrack.makeClone(), false)
                        } else {
                            logger.warn("Single track loop: no current track to restart")
                        }
                    }

                    sequentialPlayback -> {
                        // 順序播放：播放下一首
                        historyIndexManager.onTrackEnd()
                    }

                    else -> {
                        // 預設行為：順序播放
                        historyIndexManager.onTrackEnd()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling track end event", e)
        }
    }

    /**
     * 音軌開始事件處理
     */
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        try {
            // Track start handling - no logging needed for routine operation
        } catch (e: Exception) {
            logger.error("Error handling track start event", e)
        }
    }

    /**
     * 清空佇列
     * 委派給 HistoryIndexManager.clear()
     */
    fun clearQueue() {
        try {
            historyIndexManager.clear()
        } catch (e: Exception) {
            logger.error("Error clearing queue", e)
        }
    }

    /**
     * 清空歷史記錄
     * 委派給 HistoryIndexManager.clear()
     */
    fun clearHistory() {
        try {
            historyIndexManager.clear()
        } catch (e: Exception) {
            logger.error("Error clearing history", e)
        }
    }

    /**
     * 獲取佇列大小
     * 委派給 HistoryIndexManager.queueSize
     */
    fun getQueueSize(): Int = historyIndexManager.queueSize

    /**
     * 獲取佇列的副本（用於顯示）
     * 委派給 HistoryIndexManager.getQueueCopy()
     */
    fun getQueueCopy(): List<AudioTrack> = historyIndexManager.getQueueCopy()

    // 播放模式相關方法
    fun setSequentialPlayback(enabled: Boolean) {
        if (enabled) {
            sequentialPlayback = true
            singleTrackLoop = false
        }
    }

    fun setSingleTrackLoop(enabled: Boolean) {
        if (enabled) {
            singleTrackLoop = true
            sequentialPlayback = false
        }
    }

    fun isSequentialPlayback(): Boolean = sequentialPlayback
    fun isSingleTrackLoop(): Boolean = singleTrackLoop
}