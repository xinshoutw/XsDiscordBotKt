package tw.xinshou.plugin.giveaway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import tw.xinshou.loader.plugin.PluginEventConfigure
import tw.xinshou.plugin.giveaway.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pushers = mutableListOf<IntervalPusher>()

    override fun load() {
        super.load()

        for (listener in config.listeners) {
            val pusher = IntervalPusher(listener.url, listener.interval, coroutineScope)
            pusher.start()
            pushers.add(pusher)
        }

        logger.info("IntervalPusher loaded.")
    }

    override fun unload() {
        pushers.forEach { it.stop() }
        coroutineScope.cancel()
        logger.info("IntervalPusher unloaded.")
    }
}
