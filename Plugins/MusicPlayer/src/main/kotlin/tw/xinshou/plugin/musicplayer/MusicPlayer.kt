package tw.xinshou.plugin.musicplayer

import com.github.topi314.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
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
import tw.xinshou.loader.builtin.messagecreator.MessageCreator
import tw.xinshou.loader.builtin.placeholder.Placeholder
import tw.xinshou.loader.util.ComponentIdManager
import tw.xinshou.loader.util.FieldType
import tw.xinshou.plugin.musicplayer.Event.componentPrefix
import tw.xinshou.plugin.musicplayer.Event.config
import tw.xinshou.plugin.musicplayer.Event.pluginDirectory
import tw.xinshou.plugin.musicplayer.model.EnhancedTrackInfo
import tw.xinshou.plugin.musicplayer.music.GuildMusicManager
import tw.xinshou.plugin.musicplayer.music.HistoryIndexManager
import tw.xinshou.plugin.musicplayer.util.MusicPlayerUtils
import tw.xinshou.plugin.musicplayer.util.MusicPlayerUtils.connectToVoiceChannel
import tw.xinshou.plugin.musicplayer.util.MusicPlayerUtils.extractEnhancedInfo
import tw.xinshou.plugin.musicplayer.util.MusicPlayerUtils.formatTime
import tw.xinshou.plugin.musicplayer.util.MusicPlayerUtils.getSubstitutor
import tw.xinshou.plugin.musicplayer.util.MusicPlayerUtils.isUserInVoiceChannel
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
    private val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
        )
    )

    private val commandMessageCreator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
    )

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: ConcurrentHashMap<Long, GuildMusicManager> = ConcurrentHashMap()
    private val searchCache: MutableMap<String, List<EnhancedTrackInfo>> =
        Collections.synchronizedMap(object : LinkedHashMap<String, List<EnhancedTrackInfo>>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<EnhancedTrackInfo>>?): Boolean {
                return size > 100 // 限制快取大小
            }
        })

    // Message tracking system
    private val trackedMessages: ConcurrentHashMap<String, TrackedMessage> = ConcurrentHashMap()
    private val userTrackedMessages: ConcurrentHashMap<String, String> = ConcurrentHashMap() // userId -> messageId
    private val messageUpdateScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    init {
        try {
            // YouTube Support
            playerManager.registerSourceManager(
                YoutubeAudioSourceManager(
                    /* allowSearch = */ true,
                    /* allowDirectVideoIds = */ true,
                    /* allowDirectPlaylistIds = */ true
                )
            )
            logger.info("YouTube source manager registered successfully")

            // SoundCloud Support
            playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault())
            logger.info("SoundCloud source manager registered successfully")

            // Spotify Support
            try {
                if (config.engines.spotify.enabled) {
                    val spotifySourceManager = SpotifySourceManager(
                        null,
                        config.engines.spotify.clientId,
                        config.engines.spotify.clientSecret,
                        config.engines.spotify.countryCode,
                        playerManager
                    )
                    playerManager.registerSourceManager(spotifySourceManager)
                    logger.info("Spotify source manager registered successfully")
                }
            } catch (e: Exception) {
                logger.warn("Failed to register Spotify source manager, using defaults", e)
            }

            // Register remote and local sources with error handling
            try {
                AudioSourceManagers.registerRemoteSources(playerManager)
                logger.info("Remote audio sources registered successfully")
            } catch (e: Exception) {
                logger.error("Failed to register remote audio sources. This may cause issues with audio playback.", e)
            }

            try {
                AudioSourceManagers.registerLocalSource(playerManager)
                logger.info("Local audio sources registered successfully")
            } catch (e: Exception) {
                logger.warn("Failed to register local audio sources", e)
            }

            logger.info("AudioPlayerManager initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize AudioPlayerManager. Audio playback may not work properly.", e)
            throw e
        }
    }

    // Message tracking methods
    private fun trackMessage(
        hook: InteractionHook,
        messageKey: String,
        locale: DiscordLocale,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val messageId = "${hook.interaction.guild?.id}-${hook.interaction.channel?.id}-${hook.interaction.id}"
        val userId = member.id

        // Clean up any existing tracked message for this user (latest takes priority)
        userTrackedMessages[userId]?.let { existingMessageId ->
            trackedMessages[existingMessageId]?.let { existingTrackedMessage ->
                // Delete the existing message
                existingTrackedMessage.hook.deleteOriginal().queue(
                    {
                        trackedMessages.remove(existingMessageId)
                    },
                    {
                        trackedMessages.remove(existingMessageId)
                    }
                )
            }
        }

        // Track the new message
        val trackedMessage = TrackedMessage(hook, messageKey, locale, member, musicManager)
        trackedMessages[messageId] = trackedMessage
        userTrackedMessages[userId] = messageId

        // Schedule periodic updates every 5 seconds for 60 seconds
        messageUpdateScheduler.scheduleAtFixedRate({
            updateTrackedMessage(messageId)
        }, 5, 5, TimeUnit.SECONDS)
    }

    private fun updateTrackedMessage(messageId: String) {
        val trackedMessage = trackedMessages[messageId] ?: return
        val currentTime = System.currentTimeMillis()
        val timeSinceLastInteraction = currentTime - trackedMessage.lastInteractionTime

        // If 60 seconds have passed since last interaction, delete the message
        if (timeSinceLastInteraction >= 60000) {
            val userId = trackedMessage.member.id
            trackedMessage.hook.deleteOriginal().queue(
                {
                    trackedMessages.remove(messageId)
                    userTrackedMessages.remove(userId)
                },
                {
                    trackedMessages.remove(messageId)
                    userTrackedMessages.remove(userId)
                }
            )
            return
        }

        // Update the message with current information
        try {
            val currentTrack = trackedMessage.musicManager.getCurrentTrack()
            if (currentTrack != null) {
                MessageEditData.fromCreateData(
                    commandMessageCreator.getCreateBuilder(
                        trackedMessage.messageKey,
                        trackedMessage.locale,
                        getSubstitutor(
                            Placeholder.get(trackedMessage.member),
                            currentTrack,
                            trackedMessage.musicManager
                        )
                    ).build()
                ).let {
                    trackedMessage.hook.editOriginal(it).queue()
                }
            }
        } catch (e: Exception) {
            // Silently handle tracked message update failures
        }
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
                    searchYouTube(input)
                        .orTimeout(10, TimeUnit.SECONDS)
                        .thenAccept { results ->
                            val choices = results.take(10).map { track ->
                                Command.Choice(
                                    "${track.title} | ${track.author}".take(100),
                                    track.uri
                                )
                            }
                            runCatching { event.replyChoices(choices).queue() }.onFailure { }
                        }
                        .exceptionally { throwable ->
                            logger.warn("Auto-complete search failed", throwable)
                            runCatching { event.replyChoices(emptyList()).queue() }.onFailure { }
                            null
                        }
                } else {
                    runCatching { event.replyChoices(emptyList()).queue() }.onFailure { }
                }
            }

            "volume" -> {
                if (event.focusedOption.name != "level") return

                val guild = event.guild ?: return
                val musicManager = getGuildAudioPlayer(guild)
                val currentVolume = musicManager.getVolume()

                val choices = listOf(
                    Command.Choice("當前音量: $currentVolume", currentVolume.toLong())
                )
                event.replyChoices(choices).queue()
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

        // Update last interaction time for tracked messages
        updateLastInteractionTime(event.hook)

        if (!isUserInVoiceChannel(member)) {
            event.hook.editOriginal("❌ 您必須在語音頻道中才能控制音樂播放！").queue()
            return
        }

        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["action"]) {
            "previous" -> {
                handlePreviousButtonHistoryIndex(event, member, musicManager)
            }

            "pause-resume" -> {
                handlePauseResumeButton(event, member, musicManager)
            }

            "skip" -> {
                handleSkipButton(event, member, musicManager)
            }

            "loop" -> {
                handleLoopButton(event, member, musicManager)
            }

            "disconnect" -> {
                handleDisconnectButton(event, guild, musicManager)
            }
        }
    }

    private fun handleJoinCommand(event: SlashCommandInteractionEvent, guild: Guild) {
        val validation = MusicPlayerUtils.validateVoiceState(event)
        if (validation is MusicPlayerUtils.ValidationResult.Error) {
            event.hook.editOriginal("❌ ${validation.message}").queue()
            return
        }

        val (_, _, voiceChannel) = validation as MusicPlayerUtils.ValidationResult.Success

        if (!connectToVoiceChannel(guild.audioManager, voiceChannel)) {
            event.hook.editOriginal("❌ 無法連接到語音頻道！").queue()
            return
        }
        event.hook.deleteOriginal().queue()
    }


    private fun handleDisconnectCommand(event: SlashCommandInteractionEvent, guild: Guild) {
        if (!guild.audioManager.isConnected) {
            event.hook.editOriginal("❌ 機器人目前沒有連接到任何語音頻道！").queue()
            return
        }

        val musicManager = getGuildAudioPlayer(guild)
        musicManager.stop()

        guild.audioManager.closeAudioConnection()
        event.hook.deleteOriginal().queue()
    }

    private fun handlePlayCommand(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        if (!isUserInVoiceChannel(member)) {
            event.hook.editOriginal("❌ 您必須先加入語音頻道才能播放音樂！").queue()
            return
        }

        val voiceChannel = member.voiceState?.channel as? VoiceChannel
        if (voiceChannel == null) {
            event.hook.editOriginal("❌ 無法找到您的語音頻道！").queue()
            return
        }

        if (!guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
            event.hook.editOriginal("❌ 機器人沒有連接到該語音頻道的權限！").queue()
            return
        }

        val query = event.getOption("query")?.asString
        if (query.isNullOrBlank()) {
            event.hook.editOriginal("❌ 請提供要播放的歌曲名稱或URL！").queue()
            return
        }

        if (!guild.audioManager.isConnected) {
            if (!connectToVoiceChannel(guild.audioManager, voiceChannel)) {
                event.hook.editOriginal("❌ 無法連接到語音頻道！").queue()
                return
            }
        }

        // 智能查詢處理
        val searchQuery = when {
            query.startsWith("http") -> query // 直接URL
            query.contains("youtube.com") || query.contains("youtu.be") -> query
            query.contains("soundcloud.com") -> query
            query.contains("spotify.com") -> query
            else -> "ytsearch:$query" // 默認YouTube搜索
        }

        playerManager.loadItemOrdered(musicManager, searchQuery, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                musicManager.playTrack(track, member)

                MessageEditData.fromCreateData(
                    commandMessageCreator.getCreateBuilder(
                        "play",
                        event.userLocale,
                        getSubstitutor(Placeholder.get(member), track, musicManager)
                    ).build()
                ).let {
                    event.hook.editOriginal(it).queue {
                        trackMessage(event.hook, "play", event.userLocale, member, musicManager)
                    }
                }
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val tracks = playlist.tracks
                if (tracks.isEmpty()) {
                    event.hook.editOriginal("❌ 播放清單為空！").queue()
                    return
                }

                // 檢查是否為搜索結果（而非真實播放清單）
                val isSearchResult = searchQuery.startsWith("ytsearch:") ||
                        searchQuery.startsWith("scsearch:") ||
                        searchQuery.startsWith("ytmsearch:")

                if (isSearchResult) {
                    // 對於搜索結果，只添加第一個（最佳匹配）
                    val bestMatch = tracks.first()
                    musicManager.playTrack(bestMatch, member)
                } else {
                    // 對於真實播放清單，添加所有歌曲
                    tracks.forEach { track ->
                        musicManager.playTrack(track, member)
                    }
                }

                MessageEditData.fromCreateData(
                    commandMessageCreator.getCreateBuilder(
                        "play",
                        event.userLocale,
                        getSubstitutor(Placeholder.get(member), tracks.first(), musicManager)
                    ).build()
                ).let {
                    event.hook.editOriginal(it).queue {
                        trackMessage(event.hook, "play", event.userLocale, member, musicManager)
                    }
                }
            }

            override fun noMatches() {
                event.hook.editOriginal("❌ 找不到符合的歌曲：$query").queue()
            }

            override fun loadFailed(exception: FriendlyException) {
                event.hook.editOriginal("❌ 載入歌曲時發生錯誤：${exception.message}").queue()
                logger.error("Failed to load track: $query", exception)
            }
        })
    }

    private fun handlePauseCommand(
        event: SlashCommandInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val track = musicManager.getCurrentTrack()
        if (track == null) {
            event.hook.editOriginal("❌ 目前沒有正在播放的歌曲！").queue()
            return
        }
        if (musicManager.isPaused()) {
            event.hook.editOriginal("⏸️ 歌曲已經暫停了！").queue()
            return
        }

        musicManager.pause()

        MessageEditData.fromCreateData(
            commandMessageCreator.getCreateBuilder(
                "pause",
                event.userLocale,
                getSubstitutor(Placeholder.get(member), track, musicManager)
            ).build()
        ).let {
            event.hook.editOriginal(it).queue {
                // Track the message for auto-update and deletion
                trackMessage(event.hook, "pause", event.userLocale, member, musicManager)
            }
        }
    }

    private fun handleResumeCommand(
        event: SlashCommandInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val track = musicManager.getCurrentTrack()
        if (track == null) {
            event.hook.editOriginal("❌ 目前沒有正在播放的歌曲！").queue()
            return
        }
        if (!musicManager.isPaused()) {
            event.hook.editOriginal("▶️ 歌曲已經在播放了！").queue()
            return
        }

        musicManager.resume()

        MessageEditData.fromCreateData(
            commandMessageCreator.getCreateBuilder(
                "resume",
                event.userLocale,
                getSubstitutor(Placeholder.get(member), track, musicManager)
            ).build()
        ).let {
            event.hook.editOriginal(it).queue {
                // Track the message for auto-update and deletion
                trackMessage(event.hook, "resume", event.userLocale, member, musicManager)
            }
        }
    }

    private fun handleStopCommand(event: SlashCommandInteractionEvent, guild: Guild, musicManager: GuildMusicManager) {
        val currentTrack = musicManager.getCurrentTrack()
        val hasQueue = musicManager.scheduler.getQueueSize() > 0

        if (currentTrack == null && !hasQueue) {
            event.hook.editOriginal("❌ 目前沒有正在播放的歌曲！").queue()
            return
        }

        musicManager.stop()
        guild.audioManager.closeAudioConnection()
        event.hook.deleteOriginal().queue()
    }

    private fun handleSkipCommand(
        event: SlashCommandInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val skipCount = event.getOption("count")?.asInt ?: 1
        val track = musicManager.getCurrentTrack()

        if (track == null) {
            event.hook.editOriginal("❌ 目前沒有正在播放的歌曲！").queue()
            return
        }

        if (skipCount <= 0) {
            event.hook.editOriginal("❌ 跳過數量必須大於 0！").queue()
            return
        }

        try {
            // 在新的 History_index 系統中，跳過多首歌曲就是多次調用 skip()
            val queueSize = musicManager.scheduler.getQueueSize()
            val maxSkipCount = queueSize + 1 // +1 包括當前播放的歌曲

            if (skipCount > maxSkipCount) {
                event.hook.editOriginal("❌ 只能跳過 $maxSkipCount 首歌曲！").queue()
                return
            }

            // 跳過指定數量的歌曲
            for (i in 1..skipCount) {
                if (!musicManager.skip()) {
                    break // 如果沒有更多歌曲可跳過，停止
                }
            }

            // Get the next track after skipping
            val nextTrack = musicManager.getCurrentTrack()
            if (nextTrack == null) {
                event.hook.editOriginal("⏭️ 已跳過歌曲，播放清單已結束").queue()
                return
            }

            MessageEditData.fromCreateData(
                commandMessageCreator.getCreateBuilder(
                    "skip",
                    event.userLocale,
                    getSubstitutor(Placeholder.get(member), nextTrack, musicManager)
                ).build()
            ).let {
                event.hook.editOriginal(it).queue {
                    // Track the message for auto-update and deletion
                    trackMessage(event.hook, "skip", event.userLocale, member, musicManager)
                }
            }
        } catch (e: Exception) {
            logger.error("Error during skip command", e)
            event.hook.editOriginal("❌ 跳過歌曲時發生錯誤").queue()
        }
    }

    private fun handleVolumeCommand(
        event: SlashCommandInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val beforeVolume = musicManager.getVolume()
        val afterVolume = event.getOption("level")?.asInt ?: return
        musicManager.setVolume(afterVolume)

        MessageEditData.fromCreateData(
            commandMessageCreator.getCreateBuilder(
                "volume",
                event.userLocale,
                Placeholder.get(member).putAll(
                    mapOf(
                        "music_player@player_volume_before" to beforeVolume.toString(),
                        "music_player@player_volume_after" to afterVolume.toString()
                    )
                ),
            ).build()
        ).let {
            event.hook.editOriginal(it).queue {
                // Track the message for auto-update and deletion
                trackMessage(event.hook, "volume", event.userLocale, member, musicManager)
            }
        }
    }

    private fun handleQueueCommand(
        event: SlashCommandInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val currentTrack = musicManager.getCurrentTrack()
        val queue = musicManager.scheduler.getQueueCopy()

        if (currentTrack == null || queue.isEmpty()) {
            event.hook.editOriginal("❌ 播放清單為空！").queue()
            return
        }

        val queueEmbed: MessageEmbed = EmbedBuilder().apply {
            setColor(0x9bd9d5)

            val chunks = queue.chunked(10)
            chunks.forEachIndexed { chunkIndex, chunk ->
                val fieldValue = chunk.mapIndexed { index, queueTrack ->
                    val position = chunkIndex * 10 + index + 1
                    val enhancedInfo = extractEnhancedInfo(queueTrack)
                    "$position. **${enhancedInfo.title}** | ${enhancedInfo.author} `[${formatTime(queueTrack.info.length)}]`"
                }.joinToString("\n")

                addField("—", fieldValue, false)
            }
        }.build()

        MessageEditData.fromCreateData(
            commandMessageCreator.getCreateBuilder(
                "queue",
                event.userLocale,
                getSubstitutor(Placeholder.get(member), currentTrack, musicManager),
                mapOf(
                    "%music_player@player_track_list_model_key%" to queueEmbed,
                )
            ).build()
        ).let {
            event.hook.editOriginal(it).queue {
                // Track the message for auto-update and deletion
                trackMessage(event.hook, "queue", event.userLocale, member, musicManager)
            }
        }
    }

    private fun handleNowPlayingCommand(
        event: SlashCommandInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val track = musicManager.getCurrentTrack()
        if (track == null) {
            event.hook.editOriginal("❌ 目前沒有正在播放的歌曲！").queue()
            return
        }

        MessageEditData.fromCreateData(
            commandMessageCreator.getCreateBuilder(
                "now-playing",
                event.userLocale,
                getSubstitutor(Placeholder.get(member), track, musicManager)
            ).build()
        ).let {
            event.hook.editOriginal(it).queue {
                // Track the message for auto-update and deletion
                trackMessage(event.hook, "now-playing", event.userLocale, member, musicManager)
            }
        }
    }

    private fun handleShuffleCommand(
        event: SlashCommandInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        if (!isUserInVoiceChannel(member)) {
            event.hook.editOriginal("❌ 您必須在語音頻道中才能使用此指令！").queue()
            return
        }

        val historyIndexManager = musicManager.getHistoryIndexManager()
        val queueSize = historyIndexManager.queueSize

        if (queueSize <= 1) {
            event.hook.editOriginal("❌ 播放清單中沒有足夠的歌曲可以打亂！（至少需要2首歌曲）").queue()
            return
        }

        val success = historyIndexManager.shuffleQueue()
        if (success) {
            event.hook.editOriginal("🔀 已成功打亂播放清單！共打亂了 $queueSize 首歌曲。").queue()
        } else {
            event.hook.editOriginal("❌ 打亂播放清單失敗！").queue()
        }
    }


    private fun searchYouTube(query: String): CompletableFuture<List<EnhancedTrackInfo>> {
        val future = CompletableFuture<List<EnhancedTrackInfo>>()

        // 檢查快取
        searchCache[query]?.let { cachedResults ->
            future.complete(cachedResults)
            return future
        }

        val searchQuery = "ytsearch:$query"

        playerManager.loadItem(searchQuery, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                val enhancedInfo = extractEnhancedInfo(track)
                val results = listOf(enhancedInfo)
                searchCache[query] = results
                future.complete(results)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val results = playlist.tracks.take(10).map { track ->
                    extractEnhancedInfo(track)
                }
                searchCache[query] = results
                future.complete(results)
            }

            override fun noMatches() {
                future.complete(emptyList())
            }

            override fun loadFailed(exception: FriendlyException) {
                logger.warn("Search failed for query: $query", exception)
                future.complete(emptyList())
            }
        })

        return future
    }

    private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
        val guildId = guild.idLong
        return musicManagers.computeIfAbsent(guildId) {
            val musicManager = GuildMusicManager(playerManager.createPlayer(), guildId)
            guild.audioManager.sendingHandler = musicManager.sendHandler
            musicManager
        }
    }

    // Button interaction handlers

    /**
     * 處理前一首按鈕 - 使用新的 History_index 系統
     * 實現用戶期望的行為：
     * - 播放時間 >= 5 秒：重新開始當前歌曲
     * - 播放時間 < 5 秒且有上一首：播放上一首歌曲（History_index--）
     * - 播放時間 < 5 秒且無上一首：重新開始當前歌曲
     */
    private fun handlePreviousButtonHistoryIndex(
        event: ButtonInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        // 立即確認按鈕互動
        event.deferEdit().queue()

        try {
            // 使用新的 HistoryIndexManager 處理前一首按鈕邏輯
            val historyIndexManager = musicManager.getHistoryIndexManager()
            when (val result = historyIndexManager.handlePreviousButton()) {
                is HistoryIndexManager.PreviousButtonResult.TrackRestarted -> {
                    // 重新開始當前歌曲
                    val track = result.track
                    MessageEditData.fromCreateData(
                        commandMessageCreator.getCreateBuilder(
                            "now-playing",
                            event.userLocale,
                            getSubstitutor(Placeholder.get(member), track, musicManager)
                        ).build()
                    ).let { event.hook.editOriginal(it).queue() }

                    logger.debug("Previous button: restarted current track: ${track.info.title}")
                }

                is HistoryIndexManager.PreviousButtonResult.PreviousTrackPlayed -> {
                    // 播放上一首歌曲
                    val track = result.track
                    MessageEditData.fromCreateData(
                        commandMessageCreator.getCreateBuilder(
                            "now-playing",
                            event.userLocale,
                            getSubstitutor(Placeholder.get(member), track, musicManager)
                        ).build()
                    ).let { event.hook.editOriginal(it).queue() }

                    logger.debug("Previous button: played previous track: ${track.info.title}")
                }

                is HistoryIndexManager.PreviousButtonResult.Error -> {
                    // 錯誤處理
                    event.hook.editOriginal("❌ ${result.message}").queue()
                    logger.warn("Previous button error: ${result.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling previous button", e)
            event.hook.editOriginal("❌ 處理前一首按鈕時發生錯誤").queue()
        }
    }

    private fun handlePauseResumeButton(
        event: ButtonInteractionEvent,
        member: Member,
        musicManager: GuildMusicManager
    ) {
        val currentTrack = musicManager.getCurrentTrack()
        if (currentTrack == null) {
            event.hook.editOriginal("❌ 目前沒有正在播放的歌曲！").queue()
            return
        }

        val success = if (musicManager.isPaused()) {
            musicManager.resume()
        } else {
            musicManager.pause()
        }

        if (!success) {
            event.hook.editOriginal("❌ 操作失敗").queue()
            return
        }

        // Update the now-playing display with new pause/resume state
        event.deferEdit().flatMap {
            MessageEditData.fromCreateData(
                commandMessageCreator.getCreateBuilder(
                    "now-playing",
                    event.userLocale,
                    getSubstitutor(Placeholder.get(member), currentTrack, musicManager)
                ).build()
            ).let { event.hook.editOriginal(it) }
        }.queue()
    }

    private fun handleSkipButton(event: ButtonInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val currentTrack = musicManager.getCurrentTrack()
        if (currentTrack == null) {
            event.hook.editOriginal("❌ 目前沒有正在播放的歌曲！").queue()
            return
        }

        val success = musicManager.skip()
        if (!success) {
            event.hook.editOriginal("❌ 沒有更多歌曲可播放").queue()
            return
        }

        val nextTrack = musicManager.getCurrentTrack()
        if (nextTrack == null) {
            event.hook.editOriginal("⏭️ 已跳過歌曲，播放清單已結束").queue()
            return
        }

        event.deferEdit().flatMap {
            MessageEditData.fromCreateData(
                commandMessageCreator.getCreateBuilder(
                    "now-playing",
                    event.userLocale,
                    getSubstitutor(Placeholder.get(member), nextTrack, musicManager)
                ).build()
            ).let { event.hook.editOriginal(it) }
        }.queue()
    }

    private fun handleLoopButton(event: ButtonInteractionEvent, member: Member, musicManager: GuildMusicManager) {
        val scheduler = musicManager.scheduler

        // Cycle through the two distinct playback modes:
        // Sequential -> Single Track Loop -> Sequential
        when {
            scheduler.isSequentialPlayback() -> {
                // Sequential -> Single Track Loop
                scheduler.setSingleTrackLoop(true)
            }

            scheduler.isSingleTrackLoop() -> {
                // Single Track Loop -> Sequential (back to start)
                scheduler.setSequentialPlayback(true)
            }

            else -> {
                // Default fallback to Sequential
                scheduler.setSequentialPlayback(true)
            }
        }

        // Update the now-playing display with new playback mode
        val currentTrack = musicManager.getCurrentTrack()
        if (currentTrack == null) return
        event.deferEdit().flatMap {
            MessageEditData.fromCreateData(
                commandMessageCreator.getCreateBuilder(
                    "now-playing",
                    event.userLocale,
                    getSubstitutor(Placeholder.get(member), currentTrack, musicManager)
                ).build()
            ).let { event.hook.editOriginal(it) }
        }.queue()
    }

    private fun handleDisconnectButton(event: ButtonInteractionEvent, guild: Guild, musicManager: GuildMusicManager) {
        if (!guild.audioManager.isConnected) {
            event.hook.editOriginal("❌ 機器人目前沒有連接到任何語音頻道！").queue()
            return
        }

        musicManager.stop()
        guild.audioManager.closeAudioConnection()

        event.deferEdit().flatMap {
            event.hook.deleteOriginal()
        }.queue()
    }
}
