package ntustmanager

import core.config.ConfigLoader
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import ntustmanager.config.ConfigSerializer
import java.io.File

internal object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")

    internal lateinit var pluginConfig: ConfigSerializer
    internal lateinit var pluginName: String
    internal lateinit var pluginDirectory: File

    override fun PluginContext.onLoad() {
        pluginName = this.pluginName
        pluginDirectory = this.pluginDirectory
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        logger.info("Starting NTUST announcement monitoring system...")

        if (!pluginConfig.enabled) {
            logger.warn("NtustManager is disabled.")
            return
        }
        logger.info("NtustManager loaded")
        NtustManager.startSystem()
    }

    override fun PluginContext.onUnload() {
        logger.info("Shutting down NTUST announcement monitoring system...")

        if (this@Event::pluginConfig.isInitialized && pluginConfig.enabled) {
            NtustManager.shutdown()
        }

        logger.info("NtustManager unloaded")
    }

    override fun PluginContext.onReload() {
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            NtustManager.shutdown()
            logger.warn("NtustManager is disabled.")
            return
        }

        NtustManager.startSystem()
    }
}
