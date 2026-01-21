package tw.xinshou.discord.plugin.intervalpusher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.plugin.intervalpusher.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pushers = mutableListOf<IntervalPusher>()

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("IntervalPusher is disabled.")
            return
        }

        for (listener in config.listeners) {
            val pusher = IntervalPusher(listener.url, listener.interval, coroutineScope)
            pusher.start()
            pushers.add(pusher)
        }

        logger.info("IntervalPusher loaded.")
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            logger.warn("IntervalPusher is disabled.")
            return
        }

        pushers.forEach { it.stop() }

        for (listener in config.listeners) {
            val pusher = IntervalPusher(listener.url, listener.interval, coroutineScope)
            pusher.start()
            pushers.add(pusher)
        }
    }

    override fun unload() {
        pushers.forEach { it.stop() }
        coroutineScope.cancel()
        logger.info("IntervalPusher unloaded.")
    }
}
