package tw.xinshou.discord.plugin.musicplayer

import com.github.topi314.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import core.placeholder.withMember
import core.placeholder.withUser
import core.util.ComponentId
import dev.lavalink.youtube.YoutubeAudioSourceManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.plugin.musicplayer.Event.config
import tw.xinshou.discord.plugin.musicplayer.Event.pluginConfig
import tw.xinshou.discord.plugin.musicplayer.Event.pluginDirectory
import tw.xinshou.discord.plugin.musicplayer.model.EnhancedTrackInfo
import tw.xinshou.discord.plugin.musicplayer.music.GuildMusicManager
import tw.xinshou.discord.plugin.musicplayer.music.HistoryIndexManager
import tw.xinshou.discord.plugin.musicplayer.util.MusicPlayerUtils
import tw.xinshou.discord.plugin.musicplayer.util.MusicPlayerUtils.connectToVoiceChannel
import tw.xinshou.discord.plugin.musicplayer.util.MusicPlayerUtils.extractEnhancedInfo
import tw.xinshou.discord.plugin.musicplayer.util.MusicPlayerUtils.formatTime
import tw.xinshou.discord.plugin.musicplayer.util.MusicPlayerUtils.getSubstitutor
import tw.xinshou.discord.plugin.musicplayer.util.MusicPlayerUtils.isUserInVoiceChannel
import java.io.File
import java.util.*
import java.util.concurrent.*

data class TrackedMessage(
    val hook: InteractionHook,
    val messageKey: String,
    val locale: DiscordLocale,
    val member: Member,
    val musicManager: GuildMusicManager,
    var lastInteractionTime: Long = System.currentTimeMillis()
)

object MusicPlayer {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val componentId = ComponentId(
        prefix = config.componentPrefix,
        idKeys = mapOf("action" to ComponentId.FieldType.STRING)
    )

    private var messageTemplate = MessageTemplate(
        langDir = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdPrefix = config.componentPrefix,
    )

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: ConcurrentHashMap<Long, GuildMusicManager> = ConcurrentHashMap()
    private val searchCache: MutableMap<String, List<EnhancedTrackInfo>> =
        Collections.synchronizedMap(object : LinkedHashMap<String, List<EnhancedTrackInfo>>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<EnhancedTrackInfo>>?) = size > 100
        })

    private val trackedMessages: ConcurrentHashMap<String, TrackedMessage> = ConcurrentHashMap()
    private val userTrackedMessages: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val messageUpdateScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    init {
        try {
            playerManager.registerSourceManager(YoutubeAudioSourceManager(true, true, true))
            playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault())
            try {
                if (pluginConfig.engines.spotify.enabled) {
                    playerManager.registerSourceManager(SpotifySourceManager(null, pluginConfig.engines.spotify.clientId, pluginConfig.engines.spotify.clientSecret, pluginConfig.engines.spotify.countryCode, playerManager))
                }
            } catch (e: Exception) { logger.warn("Failed to register Spotify source manager", e) }
            try { AudioSourceManagers.registerRemoteSources(playerManager) } catch (e: Exception) { logger.error("Failed to register remote audio sources.", e) }
            try { AudioSourceManagers.registerLocalSource(playerManager) } catch (e: Exception) { logger.warn("Failed to register local audio sources", e) }
        } catch (e: Exception) { logger.error("Failed to initialize AudioPlayerManager.", e); throw e }
    }

    internal fun reload() {
        messageTemplate = MessageTemplate(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdPrefix = config.componentPrefix,
        )
        searchCache.clear()
    }

    private fun trackMessage(hook: InteractionHook, messageKey: String, locale: DiscordLocale, member: Member, musicManager: GuildMusicManager) {
        val messageId = "${hook.interaction.guild?.id}-${hook.interaction.channel?.id}-${hook.interaction.id}"
        val userId = member.id
        userTrackedMessages[userId]?.let { existingMessageId ->
            trackedMessages[existingMessageId]?.let { it.hook.deleteOriginal().queue({}, {}) }
            trackedMessages.remove(existingMessageId)
        }
        trackedMessages[messageId] = TrackedMessage(hook, messageKey, locale, member, musicManager)
        userTrackedMessages[userId] = messageId
        messageUpdateScheduler.scheduleAtFixedRate({ updateTrackedMessage(messageId) }, 5, 5, TimeUnit.SECONDS)
    }

    private fun updateTrackedMessage(messageId: String) {
        val trackedMessage = trackedMessages[messageId] ?: return
        if (System.currentTimeMillis() - trackedMessage.lastInteractionTime >= 60000) {
            trackedMessage.hook.deleteOriginal().queue({ trackedMessages.remove(messageId); userTrackedMessages.remove(trackedMessage.member.id) }, { trackedMessages.remove(messageId); userTrackedMessages.remove(trackedMessage.member.id) })
            return
        }
        try {
            val currentTrack = trackedMessage.musicManager.getCurrentTrack() ?: return
            val substitutor = Substitutor().withUser(trackedMessage.member.user).withMember(trackedMessage.member)
            MessageEditData.fromCreateData(messageTemplate.buildCreate(trackedMessage.messageKey, trackedMessage.locale, getSubstitutor(substitutor, currentTrack, trackedMessage.musicManager)).build()).let { trackedMessage.hook.editOriginal(it).queue() }
        } catch (_: Exception) {}
    }

    private fun updateLastInteractionTime(hook: InteractionHook) {
        val messageId = "${hook.interaction.guild?.id}-${hook.interaction.channel?.id}-${hook.interaction.id}"
        trackedMessages[messageId]?.lastInteractionTime = System.currentTimeMillis()
    }

    fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        when (event.name) {
            "play" -> {
                if (event.focusedOption.name != "query") return
                val input = event.focusedOption.value
                if (input.length >= 2) {
                    searchYouTube(input).orTimeout(10, TimeUnit.SECONDS).thenAccept { results ->
                        val choices = results.take(10).map { Command.Choice("${it.title} | ${it.author}".take(100), it.uri) }
                        runCatching { event.replyChoices(choices).queue() }
                    }.exceptionally { runCatching { event.replyChoices(emptyList()).queue() }; null }
                } else { runCatching { event.replyChoices(emptyList()).queue() } }
            }
            "volume" -> {
                if (event.focusedOption.name != "level") return
                val guild = event.guild ?: return
                val musicManager = getGuildAudioPlayer(guild)
                event.replyChoices(listOf(Command.Choice("Current: ${musicManager.getVolume()}", musicManager.getVolume().toLong()))).queue()
            }
        }
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val member = event.member ?: return
        val musicManager = getGuildAudioPlayer(guild)
        when (event.name) {
            "join" -> handleJoinCommand(event, guild)
            "disconnect" -> handleDisconnectCommand(event, guild)
            "play" -> handlePlayCommand(event, guild, member, musicManager)
            "pause" -> handlePauseCommand(event, member, musicManager)
            "resume" -> handleResumeCommand(event, member, musicManager)
            "stop" -> handleStopCommand(event, guild, musicManager)
            "skip" -> handleSkipCommand(event, member, musicManager)
            "volume" -> handleVolumeCommand(event, member, musicManager)
            "queue" -> handleQueueCommand(event, member, musicManager)
            "shuffle" -> handleShuffleCommand(event, member, musicManager)
            "now-playing" -> handleNowPlayingCommand(event, member, musicManager)
        }
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val guild = event.guild ?: return
        val member = event.member ?: return
        val musicManager = getGuildAudioPlayer(guild)
        updateLastInteractionTime(event.hook)
        if (!isUserInVoiceChannel(member)) { event.hook.editOriginal("You must be in a voice channel!").queue(); return }
        val idMap = componentId.parse(event.componentId)
        when (idMap["action"]) {
            "previous" -> handlePreviousButtonHistoryIndex(event, member, musicManager)
            "pause-resume" -> handlePauseResumeButton(event, member, musicManager)
            "skip" -> handleSkipButton(event, member, musicManager)
            "loop" -> handleLoopButton(event, member, musicManager)
            "disconnect" -> handleDisconnectButton(event, guild, musicManager)
        }
    }

    private fun handleJoinCommand(event: SlashCommandInteractionEvent, guild: Guild) {
        val validation = MusicPlayerUtils.validateVoiceState(event)
        if (validation is MusicPlayerUtils.ValidationResult.Error) { event.hook.editOriginal("Error: ${validation.message}").queue(); return }
        val (_, _, voiceChannel) = validation as MusicPlayerUtils.ValidationResult.Success
        if (!connectToVoiceChannel(guild.audioManager, voiceChannel)) { event.hook.editOriginal("Cannot connect!").queue(); return }
        event.hook.deleteOriginal().queue()
    }

    private fun handleDisconnectCommand(event: SlashCommandInteractionEvent, guild: Guild) {
        if (!guild.audioManager.isConnected) { event.hook.editOriginal("Not connected!").queue(); return }
        getGuildAudioPlayer(guild).stop(); guild.audioManager.closeAudioConnection(); event.hook.deleteOriginal().queue()
    }

    private fun handlePlayCommand(event: SlashCommandInteractionEvent, guild: Guild, member: Member, musicManager: GuildMusicManager) {
        if (!isUserInVoiceChannel(member)) { event.hook.editOriginal("Join a voice channel first!").queue(); return }
        val voiceChannel = member.voiceState?.channel as? VoiceChannel ?: return
        if (!guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) { event.hook.editOriginal("No permission!").queue(); return }
        val query = event.getOption("query")?.asString ?: return
        if (!guild.audioManager.isConnected) { if (!connectToVoiceChannel(guild.audioManager, voiceChannel)) return }
        val searchQuery = if (query.startsWith("http")) query else "ytsearch:$query"
        playerManager.loadItemOrdered(musicManager, searchQuery, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                musicManager.playTrack(track, member)
                val substitutor = Substitutor().withUser(member.user).withMember(member)
                MessageEditData.fromCreateData(messageTemplate.buildCreate("play", event.userLocale, getSubstitutor(substitutor, track, musicManager)).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "play", event.userLocale, member, musicManager) } }
            }
            override fun playlistLoaded(playlist: AudioPlaylist) {
                val tracks = playlist.tracks; if (tracks.isEmpty()) return
                val isSearchResult = searchQuery.startsWith("ytsearch:") || searchQuery.startsWith("scsearch:")
                if (isSearchResult) musicManager.playTrack(tracks.first(), member)
                else tracks.forEach { musicManager.playTrack(it, member) }
                val substitutor = Substitutor().withUser(member.user).withMember(member)
                MessageEditData.fromCreateData(messageTemplate.buildCreate("play", event.userLocale, getSubstitutor(substitutor, tracks.first(), musicManager)).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "play", event.userLocale, member, musicManager) } }
            }
            override fun noMatches() { event.hook.editOriginal("No matches: $query").queue() }
            override fun loadFailed(exception: FriendlyException) { event.hook.editOriginal("Load failed: ${exception.message}").queue() }
        })
    }

    private fun handlePauseCommand(event: SlashCommandInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val track = musicManager.getCurrentTrack() ?: run { event.hook.editOriginal("Nothing playing!").queue(); return }
        musicManager.pause()
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        MessageEditData.fromCreateData(messageTemplate.buildCreate("pause", event.userLocale, getSubstitutor(substitutor, track, musicManager)).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "pause", event.userLocale, member, musicManager) } }
    }

    private fun handleResumeCommand(event: SlashCommandInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val track = musicManager.getCurrentTrack() ?: run { event.hook.editOriginal("Nothing playing!").queue(); return }
        musicManager.resume()
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        MessageEditData.fromCreateData(messageTemplate.buildCreate("resume", event.userLocale, getSubstitutor(substitutor, track, musicManager)).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "resume", event.userLocale, member, musicManager) } }
    }

    private fun handleStopCommand(event: SlashCommandInteractionEvent, guild: Guild, musicManager: GuildMusicManager) { musicManager.stop(); guild.audioManager.closeAudioConnection(); event.hook.deleteOriginal().queue() }

    private fun handleSkipCommand(event: SlashCommandInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val skipCount = event.getOption("count")?.asInt ?: 1; val track = musicManager.getCurrentTrack() ?: run { event.hook.editOriginal("Nothing playing!").queue(); return }
        for (i in 1..skipCount) { if (!musicManager.skip()) break }
        val nextTrack = musicManager.getCurrentTrack() ?: run { event.hook.editOriginal("Queue ended.").queue(); return }
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        MessageEditData.fromCreateData(messageTemplate.buildCreate("skip", event.userLocale, getSubstitutor(substitutor, nextTrack, musicManager)).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "skip", event.userLocale, member, musicManager) } }
    }

    private fun handleVolumeCommand(event: SlashCommandInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val beforeVolume = musicManager.getVolume(); val afterVolume = event.getOption("level")?.asInt ?: return; musicManager.setVolume(afterVolume)
        val substitutor = Substitutor().withUser(member.user).withMember(member).putAll("music_player@player_volume_before" to beforeVolume.toString(), "music_player@player_volume_after" to afterVolume.toString())
        MessageEditData.fromCreateData(messageTemplate.buildCreate("volume", event.userLocale, substitutor).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "volume", event.userLocale, member, musicManager) } }
    }

    private fun handleQueueCommand(event: SlashCommandInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val currentTrack = musicManager.getCurrentTrack(); val queue = musicManager.scheduler.getQueueCopy()
        if (currentTrack == null || queue.isEmpty()) { event.hook.editOriginal("Queue is empty!").queue(); return }
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        MessageEditData.fromCreateData(messageTemplate.buildCreate("queue", event.userLocale, getSubstitutor(substitutor, currentTrack, musicManager)).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "queue", event.userLocale, member, musicManager) } }
    }

    private fun handleNowPlayingCommand(event: SlashCommandInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val track = musicManager.getCurrentTrack() ?: run { event.hook.editOriginal("Nothing playing!").queue(); return }
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        MessageEditData.fromCreateData(messageTemplate.buildCreate("now-playing", event.userLocale, getSubstitutor(substitutor, track, musicManager)).build()).let { event.hook.editOriginal(it).queue { trackMessage(event.hook, "now-playing", event.userLocale, member, musicManager) } }
    }

    private fun handleShuffleCommand(event: SlashCommandInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        if (!isUserInVoiceChannel(member)) { event.hook.editOriginal("Join voice channel!").queue(); return }
        val success = musicManager.getHistoryIndexManager().shuffleQueue()
        event.hook.editOriginal(if (success) "Shuffled!" else "Not enough songs.").queue()
    }

    private fun handlePreviousButtonHistoryIndex(event: ButtonInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        event.deferEdit().queue()
        val result = musicManager.getHistoryIndexManager().handlePreviousButton()
        val track = when (result) {
            is HistoryIndexManager.PreviousButtonResult.TrackRestarted -> result.track
            is HistoryIndexManager.PreviousButtonResult.PreviousTrackPlayed -> result.track
            is HistoryIndexManager.PreviousButtonResult.Error -> { event.hook.editOriginal("Error: ${result.message}").queue(); return }
        }
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        MessageEditData.fromCreateData(messageTemplate.buildCreate("now-playing", event.userLocale, getSubstitutor(substitutor, track, musicManager)).build()).let { event.hook.editOriginal(it).queue() }
    }

    private fun handlePauseResumeButton(event: ButtonInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val currentTrack = musicManager.getCurrentTrack() ?: return
        if (musicManager.isPaused()) musicManager.resume() else musicManager.pause()
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        event.deferEdit().flatMap { MessageEditData.fromCreateData(messageTemplate.buildCreate("now-playing", event.userLocale, getSubstitutor(substitutor, currentTrack, musicManager)).build()).let { event.hook.editOriginal(it) } }.queue()
    }

    private fun handleSkipButton(event: ButtonInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        musicManager.skip(); val nextTrack = musicManager.getCurrentTrack() ?: run { event.hook.editOriginal("Queue ended.").queue(); return }
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        event.deferEdit().flatMap { MessageEditData.fromCreateData(messageTemplate.buildCreate("now-playing", event.userLocale, getSubstitutor(substitutor, nextTrack, musicManager)).build()).let { event.hook.editOriginal(it) } }.queue()
    }

    private fun handleLoopButton(event: ButtonInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val scheduler = musicManager.scheduler
        when { scheduler.isSequentialPlayback() -> scheduler.setSingleTrackLoop(true); scheduler.isSingleTrackLoop() -> scheduler.setSequentialPlayback(true); else -> scheduler.setSequentialPlayback(true) }
        val currentTrack = musicManager.getCurrentTrack() ?: return
        val substitutor = Substitutor().withUser(member.user).withMember(member)
        event.deferEdit().flatMap { MessageEditData.fromCreateData(messageTemplate.buildCreate("now-playing", event.userLocale, getSubstitutor(substitutor, currentTrack, musicManager)).build()).let { event.hook.editOriginal(it) } }.queue()
    }

    private fun handleDisconnectButton(event: ButtonInteractionEvent, guild: Guild, musicManager: GuildMusicManager) {
        if (!guild.audioManager.isConnected) return; musicManager.stop(); guild.audioManager.closeAudioConnection()
        event.deferEdit().flatMap { event.hook.deleteOriginal() }.queue()
    }

    private fun searchYouTube(query: String): CompletableFuture<List<EnhancedTrackInfo>> {
        val future = CompletableFuture<List<EnhancedTrackInfo>>()
        searchCache[query]?.let { future.complete(it); return future }
        playerManager.loadItem("ytsearch:$query", object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) { val r = listOf(extractEnhancedInfo(track)); searchCache[query] = r; future.complete(r) }
            override fun playlistLoaded(playlist: AudioPlaylist) { val r = playlist.tracks.take(10).map { extractEnhancedInfo(it) }; searchCache[query] = r; future.complete(r) }
            override fun noMatches() { future.complete(emptyList()) }
            override fun loadFailed(exception: FriendlyException) { future.complete(emptyList()) }
        })
        return future
    }

    private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
        val musicManager = musicManagers.computeIfAbsent(guild.idLong) { GuildMusicManager(playerManager.createPlayer(), guild.idLong) }
        guild.audioManager.sendingHandler = musicManager.sendHandler
        return musicManager
    }
}
