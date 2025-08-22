package tw.xinshou.plugin.api.sqlite

import tw.xinshou.core.plugin.PluginEvent

object Event : PluginEvent(false) {
    override fun load() {
        Class.forName("org.sqlite.JDBC")
        logger.info("SQLiteAPI loaded.")
    }
}
