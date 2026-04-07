package tw.xinshou.discord.plugin.musicplayer.util

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import core.placeholder.Substitutor
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.managers.AudioManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.plugin.musicplayer.Event.pluginConfig
import tw.xinshou.discord.plugin.musicplayer.model.EnhancedTrackInfo
import tw.xinshou.discord.plugin.musicplayer.model.MusicSource
import tw.xinshou.discord.plugin.musicplayer.music.GuildMusicManager

internal object MusicPlayerUtils {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun isUserInVoiceChannel(member: Member?): Boolean = member?.voiceState?.channel != null

    fun getUserVoiceChannel(member: Member?): VoiceChannel? = member?.voiceState?.channel as? VoiceChannel

    internal fun connectToVoiceChannel(audioManager: AudioManager, voiceChannel: VoiceChannel): Boolean {
        return try { audioManager.openAudioConnection(voiceChannel); true }
        catch (e: Exception) { logger.error("Failed to connect to voice channel: ${voiceChannel.name}", e); false }
    }

    fun validateVoiceState(event: SlashCommandInteractionEvent): ValidationResult {
        val member = event.member ?: return ValidationResult.Error("Cannot get user info")
        val guild = event.guild ?: return ValidationResult.Error("Cannot get guild info")
        if (!isUserInVoiceChannel(member)) return ValidationResult.Error("You must join a voice channel first!")
        val voiceChannel = getUserVoiceChannel(member) ?: return ValidationResult.Error("Cannot find your voice channel!")
        return ValidationResult.Success(guild, member, voiceChannel)
    }

    internal fun formatTime(milliseconds: Long): String {
        if (milliseconds == Long.MAX_VALUE) return "LIVE"
        val seconds = milliseconds / 1000; val minutes = seconds / 60; val hours = minutes / 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else String.format("%02d:%02d", minutes, seconds % 60)
    }

    internal fun extractEnhancedInfo(track: AudioTrack): EnhancedTrackInfo {
        val info = track.info
        var artworkUrl: String? = null
        var source: MusicSource = MusicSource.UNKNOWN
        when {
            info.uri.contains("youtube.com") || info.uri.contains("youtu.be") -> {
                val videoId = extractYouTubeVideoId(info.uri) ?: info.identifier
                artworkUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                source = MusicSource.YOUTUBE
            }
            info.uri.contains("soundcloud.com") -> { artworkUrl = track.getUserData(String::class.java); source = MusicSource.SOUNDCLOUD }
            info.uri.contains("spotify.com") -> { artworkUrl = track.getUserData(String::class.java); source = MusicSource.SPOTIFY }
        }
        return EnhancedTrackInfo(title = info.title, author = info.author, duration = info.length, uri = info.uri, identifier = info.identifier, artworkUrl = artworkUrl, isStream = info.isStream, position = track.position, source = source)
    }

    internal fun getSubstitutor(baseSubstitutor: Substitutor, track: AudioTrack, musicManager: GuildMusicManager): Substitutor {
        return baseSubstitutor.apply {
            extractEnhancedInfo(track).let { info ->
                put("music_player@song_author_name", info.author)
                put("music_player@song_name", info.title)
                put("music_player@song_url", info.uri)
                put("music_player@song_thumbnail_url", info.artworkUrl ?: "https://i.meee.com.tw/GHexAW6.png")
                put("music_player@track_duration", formatTime(info.duration))
                put("music_player@song_duration", "${formatTime(info.position)} / ${formatTime(info.duration)}")
            }
            try {
                val requesterData = track.userData as? Map<*, *>
                if (requesterData != null) {
                    put("music_player@song_requester_username", requesterData["requesterUsername"]?.toString() ?: "Unknown")
                    put("music_player@song_requester_name", requesterData["requesterName"]?.toString() ?: "Unknown")
                    put("music_player@song_requester_avatar_url", requesterData["requesterAvatarUrl"]?.toString() ?: "")
                    put("music_player@song_requester_id", requesterData["requesterId"]?.toString() ?: "")
                } else {
                    put("music_player@song_requester_username", "Unknown"); put("music_player@song_requester_name", "Unknown")
                    put("music_player@song_requester_avatar_url", ""); put("music_player@song_requester_id", "")
                }
            } catch (_: Exception) {
                put("music_player@song_requester_username", "Unknown"); put("music_player@song_requester_name", "Unknown")
                put("music_player@song_requester_avatar_url", ""); put("music_player@song_requester_id", "")
            }
            put("music_player@player_current_color", if (musicManager.isPlaying()) "0x68c06e" else "0xba9c55")
        }
    }

    fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com(?:\\.\\w{2})?/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]+)",
            "youtube\\.com(?:\\.\\w{2})?/embed/([a-zA-Z0-9_-]+)",
            "youtube\\.com(?:\\.\\w{2})?/v/([a-zA-Z0-9_-]+)"
        )
        for (pattern in patterns) {
            val matchResult = Regex(pattern).find(url)
            if (matchResult != null) return matchResult.groupValues[1]
        }
        return null
    }

    sealed class ValidationResult {
        data class Success(val guild: Guild, val member: Member, val voiceChannel: VoiceChannel) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
