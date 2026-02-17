package tw.xinshou.discord.webdashboard.api

import tw.xinshou.discord.webdashboard.model.CoreConfigDto
import tw.xinshou.discord.webdashboard.model.PluginConfigDto
import tw.xinshou.discord.webdashboard.model.PluginYamlDto
import tw.xinshou.discord.webdashboard.model.SaveResponseDto

interface DashboardBackend {
    fun mode(): String

    fun getCoreConfig(): CoreConfigDto

    fun saveCoreConfig(payload: CoreConfigDto): SaveResponseDto

    fun listPlugins(): List<PluginConfigDto>

    fun updatePluginEnabled(pluginName: String, enabled: Boolean): SaveResponseDto

    fun getPluginYaml(pluginName: String): PluginYamlDto

    fun savePluginYaml(pluginName: String, yaml: String): SaveResponseDto
}

object UnavailableDashboardBackend : DashboardBackend {
    private const val ERROR_MESSAGE = "Dashboard backend is not configured."

    override fun mode(): String = "unconfigured"

    override fun getCoreConfig(): CoreConfigDto = throw IllegalStateException(ERROR_MESSAGE)

    override fun saveCoreConfig(payload: CoreConfigDto): SaveResponseDto = throw IllegalStateException(ERROR_MESSAGE)

    override fun listPlugins(): List<PluginConfigDto> = throw IllegalStateException(ERROR_MESSAGE)

    override fun updatePluginEnabled(pluginName: String, enabled: Boolean): SaveResponseDto =
        throw IllegalStateException(ERROR_MESSAGE)

    override fun getPluginYaml(pluginName: String): PluginYamlDto = throw IllegalStateException(ERROR_MESSAGE)

    override fun savePluginYaml(pluginName: String, yaml: String): SaveResponseDto =
        throw IllegalStateException(ERROR_MESSAGE)
}
