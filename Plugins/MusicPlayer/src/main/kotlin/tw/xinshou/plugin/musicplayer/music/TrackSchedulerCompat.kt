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

    /**
     * 音軌結束事件處理
     * 委派給 HistoryIndexManager.onTrackEnd()
     */
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        try {
            logger.debug("Track ended: {}, reason: {}", track.info.title, endReason)

            // 只在正常結束時自動播放下一首
            if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.STOPPED || endReason == AudioTrackEndReason.LOAD_FAILED) {
                historyIndexManager.onTrackEnd()
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
            logger.debug("Track started: ${track.info.title}")
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
            logger.debug("Cleared queue via HistoryIndexManager")
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
            logger.debug("Cleared history via HistoryIndexManager")
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

    // 播放模式相關方法 - 暫時保持空實現，因為新系統專注於 History_index 行為
    fun setSequentialPlayback(enabled: Boolean) {
        logger.debug("setSequentialPlayback($enabled) - not implemented in History_index system")
    }

    fun setShufflePlayback(enabled: Boolean) {
        logger.debug("setShufflePlayback($enabled) - not implemented in History_index system")
    }

    fun setSingleTrackLoop(enabled: Boolean) {
        logger.debug("setSingleTrackLoop($enabled) - not implemented in History_index system")
    }

    fun setQueueLoop(enabled: Boolean) {
        logger.debug("setQueueLoop($enabled) - not implemented in History_index system")
    }

    fun isSequentialPlayback(): Boolean = true // 默認行為
    fun isShufflePlayback(): Boolean = false
    fun isSingleTrackLoop(): Boolean = false
    fun isQueueLoop(): Boolean = false
}