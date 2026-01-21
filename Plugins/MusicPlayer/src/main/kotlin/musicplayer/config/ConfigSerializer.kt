package tw.xinshou.discord.plugin.musicplayer.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,

    @SerialName("command_message_acknowledges")
    val commandMessageAcknowledges: CommandMessageAcknowledges,
    val engines: EnginesConfig,
    val player: PlayerConfig,
    val messages: MessagesConfig,
    val features: FeaturesConfig,
    val performance: PerformanceConfig,
    val cache: CacheConfig,
    val developer: DeveloperConfig,
    val emojis: EmojisConfig
) {
    @Serializable
    data class CommandMessageAcknowledges(
        val join: Boolean,
        val disconnect: Boolean,
        val stop: Boolean,
        val play: Boolean,
        val pause: Boolean,
        val resume: Boolean,
        val skip: Boolean,
        val queue: Boolean,
        val volume: Boolean,
        @SerialName("now_playing")
        val nowPlaying: Boolean
    )

    @Serializable
    data class EnginesConfig(
        val youtube: YoutubeConfig,
        val soundcloud: SoundCloudConfig,
        val spotify: SpotifyConfig
    ) {

        @Serializable
        data class YoutubeConfig(
            val enabled: Boolean,
            @SerialName("search_enabled")
            val searchEnabled: Boolean,
            @SerialName("auto_complete")
            val autoComplete: Boolean,
            @SerialName("max_search_results")
            val maxSearchResults: Int
        ) {
            init {
                require(maxSearchResults in 1..25) {
                    "Max search results must be between 1-25, got: $maxSearchResults"
                }
            }
        }

        @Serializable
        data class SoundCloudConfig(
            val enabled: Boolean,
            @SerialName("client_id")
            val clientId: String
        )

        @Serializable
        data class SpotifyConfig(
            val enabled: Boolean,
            @SerialName("client_id")
            val clientId: String,
            @SerialName("client_secret")
            val clientSecret: String,
            @SerialName("country_code")
            val countryCode: String
        ) {
            init {
                if (enabled) {
                    require(clientId.isNotBlank()) { "Spotify client ID is required when Spotify is enabled" }
                    require(clientSecret.isNotBlank()) { "Spotify client secret is required when Spotify is enabled" }
                    require(countryCode.length == 2) { "Country code must be 2 characters, got: $countryCode" }
                }
            }
        }
    }

    @Serializable
    data class PlayerConfig(
        @SerialName("default_volume")
        val defaultVolume: Int,
        @SerialName("auto_leave_timeout")
        val autoLeaveTimeout: Int,
        @SerialName("max_queue_size")
        val maxQueueSize: Int,
        @SerialName("search_timeout")
        val searchTimeout: Int,
        val playback: PlaybackConfig,
        val cache: PlayerCacheConfig
    ) {
        init {
            require(defaultVolume in 0..100) { "Default volume must be between 0-100, got: $defaultVolume" }
            require(autoLeaveTimeout >= 0) { "Auto leave timeout must be non-negative, got: $autoLeaveTimeout" }
            require(maxQueueSize > 0) { "Max queue size must be positive, got: $maxQueueSize" }
            require(searchTimeout > 0) { "Search timeout must be positive, got: $searchTimeout" }
        }

        @Serializable
        data class PlaybackConfig(
            @SerialName("enable_lyrics")
            val enableLyrics: Boolean,
            @SerialName("show_artwork")
            val showArtwork: Boolean,
            @SerialName("show_author_avatar")
            val showAuthorAvatar: Boolean,
            @SerialName("lyrics_preview_length")
            val lyricsPreviewLength: Int
        ) {
            init {
                require(lyricsPreviewLength > 0) {
                    "Lyrics preview length must be positive, got: $lyricsPreviewLength"
                }
            }
        }

        @Serializable
        data class PlayerCacheConfig(
            @SerialName("search_cache_size")
            val searchCacheSize: Int,
            @SerialName("search_cache_ttl")
            val searchCacheTtl: Int,
            @SerialName("artwork_cache_enabled")
            val artworkCacheEnabled: Boolean
        ) {
            init {
                require(searchCacheSize > 0) {
                    "Search cache size must be positive, got: $searchCacheSize"
                }
                require(searchCacheTtl > 0) {
                    "Search cache TTL must be positive, got: $searchCacheTtl"
                }
            }
        }
    }

    @Serializable
    data class MessagesConfig(
        @SerialName("show_detailed_errors")
        val showDetailedErrors: Boolean = true,
        @SerialName("announce_new_track")
        val announceNewTrack: Boolean = true,
        @SerialName("use_enhanced_embeds")
        val useEnhancedEmbeds: Boolean = true,
        val embeds: EmbedsConfig = EmbedsConfig()
    ) {

        @Serializable
        data class EmbedsConfig(
            @SerialName("show_thumbnails")
            val showThumbnails: Boolean = true,
            @SerialName("show_progress_bar")
            val showProgressBar: Boolean = false,
            @SerialName("color_scheme")
            val colorScheme: String = "dynamic"
        ) {
            init {
                val validColorSchemes = setOf("dynamic", "blue", "green", "purple", "red", "orange")
                require(colorScheme in validColorSchemes) {
                    "Invalid color scheme: $colorScheme. Valid options: $validColorSchemes"
                }
            }
        }
    }

    @Serializable
    data class FeaturesConfig(
        @SerialName("auto_complete")
        val autoComplete: Boolean = true,
        @SerialName("lyrics_support")
        val lyricsSupport: Boolean = true,
        @SerialName("artwork_display")
        val artworkDisplay: Boolean = true,
        @SerialName("multi_skip")
        val multiSkip: Boolean = true,
        @SerialName("volume_control")
        val volumeControl: Boolean = true
    )

    @Serializable
    data class PerformanceConfig(
        @SerialName("max_concurrent_searches")
        val maxConcurrentSearches: Int = 5,
        @SerialName("search_thread_pool_size")
        val searchThreadPoolSize: Int = 3,
        @SerialName("audio_send_system")
        val audioSendSystem: String = "JDA"
    ) {
        init {
            require(maxConcurrentSearches > 0) {
                "Max concurrent searches must be positive, got: $maxConcurrentSearches"
            }
            require(searchThreadPoolSize > 0) {
                "Search thread pool size must be positive, got: $searchThreadPoolSize"
            }
            val validAudioSystems = setOf("JDA", "NAS")
            require(audioSendSystem in validAudioSystems) {
                "Invalid audio send system: $audioSendSystem. Valid options: $validAudioSystems"
            }
        }
    }

    @Serializable
    data class CacheConfig(
        val size: Int = 100,
        @SerialName("expire_time")
        val expireTime: Int = 24
    ) {
        init {
            require(size > 0) { "Cache size must be positive, got: $size" }
            require(expireTime > 0) { "Cache expire time must be positive, got: $expireTime" }
        }
    }

    @Serializable
    data class DeveloperConfig(
        @SerialName("debug_mode")
        val debugMode: Boolean = false,
        @SerialName("log_search_queries")
        val logSearchQueries: Boolean = false,
        @SerialName("log_audio_events")
        val logAudioEvents: Boolean = false
    )

    @Serializable
    data class EmojisConfig(
        @SerialName("progress_white_start")
        val progressWhiteStartName: String = "music_player__line_white_start",
        @SerialName("progress_white_full")
        val progressWhiteFullName: String = "music_player__line_white_full",
        @SerialName("progress_white_end")
        val progressWhiteEndName: String = "music_player__line_white_end",
        @SerialName("progress_black")
        val progressBlackName: String = "music_player__line_black",
        @SerialName("progress_black_end")
        val progressBlackEndName: String = "music_player__line_black_end",
        @SerialName("media_play")
        val mediaPlayName: String = "music_player__media_play",
        @SerialName("media_pause")
        val mediaPauseName: String = "music_player__media_pause",
        @SerialName("media_ordered")
        val mediaOrderedName: String = "music_player__media_ordered",
        @SerialName("media_repeat_once")
        val mediaRepeatOnceName: String = "music_player__media_repeat_once"
    )

    /**
     * 驗證整個配置的一致性
     */
    fun validate() {
        // 如果啟用自動完成但禁用 YouTube 搜索，則警告
        if (features.autoComplete && (!engines.youtube.enabled || !engines.youtube.searchEnabled)) {
            throw IllegalStateException("Auto-complete requires YouTube search to be enabled")
        }

        // 如果啟用歌詞但禁用歌詞支援，則警告
        if (player.playback.enableLyrics && !features.lyricsSupport) {
            throw IllegalStateException("Lyrics playback requires lyrics support to be enabled")
        }

        // 如果啟用封面圖片但禁用封面顯示，則警告
        if (player.playback.showArtwork && !features.artworkDisplay) {
            throw IllegalStateException("Artwork playback requires artwork display to be enabled")
        }
    }

    /**
     * 獲取啟用的音源列表
     */
    fun getEnabledSources(): List<String> {
        return buildList {
            if (engines.youtube.enabled) add("YouTube")
            if (engines.soundcloud.enabled) add("SoundCloud")
            if (engines.spotify.enabled) add("Spotify")
        }
    }

    /**
     * 獲取啟用的功能列表
     */
    fun getEnabledFeatures(): List<String> {
        return buildList {
            if (features.autoComplete) add("Auto-complete")
            if (features.lyricsSupport) add("Lyrics Support")
            if (features.artworkDisplay) add("Artwork Display")
            if (features.multiSkip) add("Multi-skip")
            if (features.volumeControl) add("Volume Control")
        }
    }
}