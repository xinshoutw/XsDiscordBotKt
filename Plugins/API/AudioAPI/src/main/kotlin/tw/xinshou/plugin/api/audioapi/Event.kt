package tw.xinshou.plugin.api.audioapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.plugin.PluginEvent


object Event : PluginEvent(false) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun load() {
        logger.info("AudioAPI loaded.")
    }

    override fun unload() {
        logger.info("AudioAPI unloaded.")
    }
}