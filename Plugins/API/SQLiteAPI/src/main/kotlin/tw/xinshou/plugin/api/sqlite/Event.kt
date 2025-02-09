package tw.xinshou.plugin.api.sqlite

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.plugin.PluginEvent

object Event : PluginEvent(false) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun load() {
        Class.forName("org.sqlite.JDBC")
        logger.info("SQLiteAPI loaded.")
    }

    override fun unload() {
        logger.info("SQLiteAPI unloaded.")
    }
}
