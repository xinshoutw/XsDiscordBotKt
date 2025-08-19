package tw.xinshou.plugin.ntustmanager

import tw.xinshou.loader.plugin.PluginEvent

object Event : PluginEvent(false) {

    override fun load() {
        super.load()

        // Initialize NtustManager - this will start the scheduler and initialize cache
        logger.info("Starting NTUST announcement monitoring system...")
        NtustManager

        // The NtustManager object is initialized automatically when first accessed
        // due to its init block, which starts the scheduler and loads cache data
        logger.info("NTUST Manager loaded successfully with cache integration")
    }

    override fun unload() {
        logger.info("Shutting down NTUST announcement monitoring system...")

        // Shutdown the NtustManager scheduler and cleanup resources
        NtustManager.shutdown()

        super.unload()
        logger.info("NTUST Manager unloaded successfully")
    }

    override fun reload() {
        logger.info("Reloading NTUST announcement monitoring system...")

        // Shutdown current instance
        NtustManager.shutdown()

        // The system will be reinitialized on next access
        super.reload()

        logger.info("NTUST Manager reloaded successfully")
    }
}