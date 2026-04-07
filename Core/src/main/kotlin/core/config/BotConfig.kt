package core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    @SerialName("bot_token")
    val botToken: String,

    val database: DatabaseConfig = DatabaseConfig(),

    @SerialName("status_changer")
    val statusChanger: StatusChangerConfig = StatusChangerConfig(),

    @SerialName("console_loggers")
    val consoleLoggers: List<ConsoleLoggerConfig> = emptyList(),

    @SerialName("auto_defer_replies")
    val autoDeferReplies: Boolean = true,
) {
    @Serializable
    data class DatabaseConfig(
        val embedded: Boolean = true,

        @SerialName("connection_string")
        val connectionString: String = "mongodb://localhost:27017",

        val port: Int = 27017,

        @SerialName("data_path")
        val dataPath: String = "mongodb-data",
    )

    @Serializable
    data class StatusChangerConfig(
        val activities: List<String> = emptyList(),
    )

    @Serializable
    data class ConsoleLoggerConfig(
        @SerialName("guild_id")
        val guildId: Long = 0,

        @SerialName("channel_id")
        val channelId: Long = 0,

        @SerialName("log_types")
        val logTypes: List<String> = emptyList(),

        val format: String = "",
    )
}
