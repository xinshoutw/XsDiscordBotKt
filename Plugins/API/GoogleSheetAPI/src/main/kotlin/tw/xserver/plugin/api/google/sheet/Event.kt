package tw.xserver.plugin.api.google.sheet

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.plugin.PluginEvent


object Event : PluginEvent(false) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun load() {
        logger.info("GoogleSheetAPI loaded.")
    }

    override fun unload() {
        logger.info("GoogleSheetAPI unloaded.")
    }
}