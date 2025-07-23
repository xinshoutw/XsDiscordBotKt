package tw.xinshou.plugin.musicplayer.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Member
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class HistoryIndexManager(private val audioPlayer: AudioPlayer) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val PREVIOUS_THRESHOLD_MS = 5000L // 5 seconds
        private const val MAX_HISTORY_SIZE = 240
    }

    // Core data structures
    private val history: MutableList<AudioTrack> = mutableListOf()
    private val historyIndex = AtomicInteger(0)

    // Derived properties (not stored separately)
    val currentTrack: AudioTrack?
        get() = history.getOrNull(historyIndex.get())

    val previousTrack: AudioTrack?
        get() = history.getOrNull(historyIndex.get() - 1)

    val queue: List<AudioTrack> // Returns a list containing all elements except first `n` elements
        get() = history.drop(historyIndex.get() + 1)

    val queueSize: Int
        get() = maxOf(0, history.size - historyIndex.get() - 1)

    val hasNext: Boolean
        get() = historyIndex.get() < history.size - 1

    val hasPrevious: Boolean
        get() = historyIndex.get() > 0


    fun addTrack(track: AudioTrack, requester: Member): Boolean {
        return try {
            // Store requester info
            track.userData = mapOf(
                "requesterId" to requester.id,
                "requesterName" to requester.effectiveName,
                "requesterUsername" to requester.user.name,
                "requesterAvatarUrl" to requester.effectiveAvatarUrl
            )

            val isFirstTrack = history.isEmpty()
            history.add(track.makeClone())

            // start immediately
            if (isFirstTrack) {
                historyIndex.set(0)

                return audioPlayer.startTrack(currentTrack!!, false).let { success ->
                    if (success) {
                        logger.debug("Started first track: ${track.info.title}")
                    } else {
                        logger.error("Failed to start first track: ${track.info.title}")
                    }
                    success
                }
            }

            // add to the end of history
            logger.debug("Added track to history: ${track.info.title} (position ${history.size - 1})")
            manageHistorySize()
            return true

        } catch (e: Exception) {
            logger.error("Error adding track: ${track.info?.title}", e)
            false
        }
    }

    fun skip(): Boolean {
        return try {
            if (!hasNext) {
                logger.debug("No next track available for skip")
                return false
            }

            historyIndex.incrementAndGet()
            val nextTrack = currentTrack
            if (nextTrack == null) {
                logger.error("No track found at index ${historyIndex.get()}")
                historyIndex.decrementAndGet()
                return false
            }

            audioPlayer.startTrack(nextTrack.makeClone(), false).let { success ->
                if (success) {
                    logger.debug("Skipped to track at index ${historyIndex.get()}: ${nextTrack.info.title}")
                } else {
                    logger.error("Failed to skip to track at index ${historyIndex.get()}: ${nextTrack.info.title}")
                    skip() // Try to skip again if it failed
                }
                success
            }
        } catch (e: Exception) {
            logger.error("Error during skip", e)
            false
        }
    }


    sealed class PreviousButtonResult {
        data class TrackRestarted(val track: AudioTrack) : PreviousButtonResult()
        data class PreviousTrackPlayed(val track: AudioTrack) : PreviousButtonResult()
        data class Error(val message: String) : PreviousButtonResult()
    }

    fun handlePreviousButton(): PreviousButtonResult {
        return try {
            // 先簡單判斷是否正在播放
            currentTrack ?: return PreviousButtonResult.Error("沒有正在播放的歌曲")

            // 取得實際已播放時長
            val playingTrack = audioPlayer.playingTrack ?: return PreviousButtonResult.Error("無法獲取當前播放狀態")
            val currentPosition = playingTrack.position

            if (!hasPrevious || currentPosition >= PREVIOUS_THRESHOLD_MS) {
                return restartCurrentTrack()
            }

            previousTrack?.let { previousTrack ->
                audioPlayer.startTrack(previousTrack.makeClone(), false).let { success ->
                    if (success) {
                        historyIndex.decrementAndGet()
                        logger.debug("Moved to previous track at index ${historyIndex.get()}: ${previousTrack.info.title}")
                        PreviousButtonResult.PreviousTrackPlayed(previousTrack)
                    } else {
                        PreviousButtonResult.Error("無法播放上一首歌曲")
                    }
                }
            } ?: run {
                PreviousButtonResult.Error("上一首歌曲不存在")
            }
        } catch (e: Exception) {
            logger.error("Error handling previous button", e)
            PreviousButtonResult.Error("處理前一首按鈕時發生錯誤：${e.message}")
        }
    }

    private fun restartCurrentTrack(): PreviousButtonResult {
        return try {
            val current = currentTrack ?: return PreviousButtonResult.Error("沒有當前歌曲可重新開始")

            audioPlayer.startTrack(current.makeClone(), false).let { success ->
                if (success) {
                    logger.debug("Restarted current track: ${current.info.title}")
                    PreviousButtonResult.TrackRestarted(current)
                } else {
                    logger.error("Failed to restart current track: ${current.info.title}")
                    PreviousButtonResult.Error("無法重新開始當前歌曲")
                }
            }
        } catch (e: Exception) {
            logger.error("Error restarting current track", e)
            PreviousButtonResult.Error("重新開始歌曲時發生錯誤")
        }
    }

    fun onTrackEnd(): Boolean {
        return try {
            if (hasNext) {
                skip()
            } else {
                logger.debug("Reached end of history, no more tracks to play")
                false
            }
        } catch (e: Exception) {
            logger.error("Error handling track end", e)
            false
        }
    }

    /**
     * Manage history size by erasing tracks before History_index
     */
    private fun manageHistorySize() { // TODO: fix overflow tracks
        try {
            if (history.size <= MAX_HISTORY_SIZE) {
                return
            }

            val tracksToRemove = history.size - MAX_HISTORY_SIZE
            val currentIdx = historyIndex.get()
            val canRemove = minOf(tracksToRemove, currentIdx)

            if (canRemove > 0) {
                // Remove tracks from beginning (before current index)
                repeat(canRemove) {
                    history.removeFirst()
                }
                // Adjust index since we removed from beginning
                historyIndex.addAndGet(-canRemove)

                logger.debug("Removed $canRemove tracks from history beginning, new index: ${historyIndex.get()}")
            } else {
                logger.warn("Cannot remove enough tracks from history beginning, history size: ${history.size}")
            }
        } catch (e: Exception) {
            logger.error("Error managing history size", e)
        }
    }

    /**
     * Get queue copy for display
     */
    fun getQueueCopy(): List<AudioTrack> = queue.map { it.makeClone() }

    /**
     * Clear all data
     */
    fun clear() {
        history.clear()
        historyIndex.set(0)
    }
}