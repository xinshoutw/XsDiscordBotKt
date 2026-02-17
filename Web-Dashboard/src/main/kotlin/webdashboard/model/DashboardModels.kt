package tw.xinshou.discord.webdashboard.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthDto(
    val status: String,
    val serverTime: Long,
    val mode: String,
    val recommendedBaseUrl: String,
)

@Serializable
data class SaveResponseDto(
    val ok: Boolean,
    val message: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class CoreConfigDto(
    val general: GeneralSettingsDto,
    val builtins: BuiltinSettingsDto,
)

@Serializable
data class GeneralSettingsDto(
    val tokenMasked: String,
)

@Serializable
data class BuiltinSettingsDto(
    val statusMessages: List<String>,
    val consoleTargets: List<ConsoleTargetDto>,
)

@Serializable
data class ConsoleTargetDto(
    val guildId: Long,
    val channelId: Long,
    val logTypes: List<String>,
    val format: String,
)

@Serializable
data class PluginConfigDto(
    val name: String,
    val enabled: Boolean,
    val category: String,
    val description: String,
    val dependencies: List<String> = emptyList(),
    val intents: List<String> = emptyList(),
    val author: String? = null,
    val version: String? = null,
    val requireIntents: List<String> = emptyList(),
    val requireCacheFlags: List<String> = emptyList(),
    val requireMemberCachePolicies: List<String> = emptyList(),
    val dependPlugins: List<String> = emptyList(),
    val softDependPlugins: List<String> = emptyList(),
    val loaded: Boolean = false,
    val canToggle: Boolean = false,
    val configPath: String? = null,
    val hasWebEditor: Boolean = false,
)

@Serializable
data class PluginToggleRequestDto(
    val enabled: Boolean,
)

@Serializable
data class PluginYamlDto(
    val name: String,
    val yaml: String,
    val path: String,
    val loaded: Boolean,
    val hasEnabledFlag: Boolean,
)

@Serializable
data class PluginYamlUpdateRequestDto(
    val yaml: String,
)
