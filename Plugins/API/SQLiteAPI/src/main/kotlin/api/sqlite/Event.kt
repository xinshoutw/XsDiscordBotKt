package tw.xinshou.discord.plugin.api.sqlite

import core.plugin.Plugin
import core.plugin.PluginConfig
import core.plugin.PluginContext
import org.slf4j.LoggerFactory


object Event : Plugin {
    override lateinit var config: PluginConfig

    private val logger = LoggerFactory.getLogger(Event::class.java)

    override fun PluginContext.onLoad() {
        Class.forName("org.sqlite.JDBC")
        logger.info("SQLiteAPI loaded.")
    }
}
