package tw.xinshou.discord.plugin.musicplayer.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

class AudioPlayerSendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler, AutoCloseable {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val BUFFER_SIZE = 1024
    }

    private val buffer: ByteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
    private val frame: MutableAudioFrame = MutableAudioFrame().apply { setBuffer(buffer) }

    override fun canProvide(): Boolean {
        return try {
            buffer.clear()
            audioPlayer.provide(frame)
        } catch (e: Exception) {
            logger.warn("Error occurred while providing audio frame", e)
            false
        }
    }

    override fun provide20MsAudio(): ByteBuffer? {
        return try {
            buffer.flip()

            if (buffer.remaining() > 0) {
                val audioBuffer = buffer.duplicate() // 創建副本避免併發問題
                logger.trace("Providing audio buffer with {} bytes", audioBuffer.remaining())
                audioBuffer
            } else {
                logger.trace("No audio data in buffer")
                null
            }
        } catch (e: Exception) {
            logger.error("Error occurred while providing 20ms audio", e)
            null
        }
    }

    override fun isOpus(): Boolean {
        return true
    }

    override fun close() {
        try {
            buffer.clear()
            logger.debug("AudioPlayerSendHandler cleaned up")
        } catch (e: Exception) {
            logger.warn("Error occurred during cleanup", e)
        }
    }
}