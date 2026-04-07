package intervalpusher

import core.config.ConfigLoader
import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import intervalpusher.config.ConfigSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File

object Event : Plugin {
    override var config: PluginConfig = PluginConfig(name = "", main = "", coreApi = "", version = "")

    internal lateinit var pluginConfig: ConfigSerializer
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pushers = mutableListOf<IntervalPusher>()

    override fun PluginContext.onLoad() {
        pluginConfig = ConfigLoader.load<ConfigSerializer>(
            File(pluginDirectory, "config.yaml"), "/config.yaml"
        )

        pushers.forEach { it.stop() }
        pushers.clear()

        if (!pluginConfig.enabled) {
            logger.warn("IntervalPusher is disabled.")
            return
        }

        for (listener in pluginConfig.listeners) {
            val pusher = IntervalPusher(listener.url, listener.interval, coroutineScope)
            pusher.start()
            pushers.add(pusher)
        }

        logger.info("IntervalPusher loaded.")
    }

    override fun PluginContext.onReload() {
        onLoad()
    }

    override fun PluginContext.onUnload() {
        pushers.forEach { it.stop() }
        coroutineScope.cancel()
        logger.info("IntervalPusher unloaded.")
    }
}
