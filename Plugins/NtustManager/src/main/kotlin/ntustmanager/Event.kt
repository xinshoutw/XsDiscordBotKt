package tw.xinshou.discord.plugin.ntustmanager

import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.plugin.ntustmanager.config.ConfigSerializer

internal object Event : PluginEventConfigure<ConfigSerializer>(false, ConfigSerializer.serializer()) {
    override fun load() {
        super.load()

        // Initialize NtustManager - this will start the scheduler and initialize cache
        logger.info("Starting NTUST announcement monitoring system...")

        if (!config.enabled) {
            logger.warn("NtustManager is disabled.")
            return
        }
        logger.info("NtustManager loaded")
        NtustManager.startSystem()
    }

    override fun unload() {
        super.unload()

        logger.info("Shutting down NTUST announcement monitoring system...")

        if (config.enabled) {
            NtustManager.shutdown()
        }

        logger.info("NtustManager unloaded")
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            NtustManager.shutdown()
            logger.warn("NtustManager is disabled.")
            return
        }

        NtustManager.startSystem()
    }
}