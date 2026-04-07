package ntustcourse

import core.command.CommandHandler
import core.config.ConfigLoader
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import ntustcourse.command.guildCommands
import ntustcourse.config.ConfigSerializer
import java.io.File

/**
 * Main class for the NtustCourse plugin managing configurations, commands, and course monitoring.
 */
object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")

    internal lateinit var pluginConfig: ConfigSerializer
    private lateinit var ctx: PluginContext

    override fun PluginContext.onLoad() {
        ctx = this
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("NtustCourse is disabled.")
            return
        }

        NtustCourse.start()
        logger.info("NtustCourse loaded.")
    }

    override fun PluginContext.onUnload() {
        if (this@Event::pluginConfig.isInitialized && pluginConfig.enabled) {
            NtustCourse.stop()
        }
        logger.info("NtustCourse unloaded.")
    }

    override fun PluginContext.onReload() {
        if (this@Event::pluginConfig.isInitialized && pluginConfig.enabled) {
            NtustCourse.stop()
        }

        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        if (!pluginConfig.enabled) {
            logger.warn("NtustCourse is disabled.")
            return
        }

        NtustCourse.start()
    }

    override fun commands(): List<CommandHandler> {
        if (!this::pluginConfig.isInitialized || !pluginConfig.enabled) return emptyList()
        return guildCommands
    }
}
