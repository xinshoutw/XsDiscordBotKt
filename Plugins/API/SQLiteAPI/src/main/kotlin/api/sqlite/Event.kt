package tw.xinshou.discord.plugin.api.sqlite

import tw.xinshou.discord.core.plugin.PluginEvent

object Event : PluginEvent(false) {
    override fun load() {
        Class.forName("org.sqlite.JDBC")
        logger.info("SQLiteAPI loaded.")
    }
}
