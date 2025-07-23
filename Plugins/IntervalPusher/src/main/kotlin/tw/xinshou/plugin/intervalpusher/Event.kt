package tw.xinshou.plugin.intervalpusher

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.plugin.intervalpusher.serializer.MainConfigSerializer
import java.io.File
import java.io.IOException

object Event : PluginEvent(true) {
    private val PLUGIN_DIR_FILE = File("plugins/IntervalPusher")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var config: MainConfigSerializer
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pushers = mutableListOf<IntervalPusher>()

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reload(true)

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

    override fun reload(init: Boolean) {
        try {
            fileGetter.readInputStream("config.yaml").use {
                config = Yaml().decodeFromStream<MainConfigSerializer>(it)
            }
        } catch (e: IOException) {
            logger.error("Please configure {}./config.yaml!", PLUGIN_DIR_FILE.canonicalPath, e)
        }

        if (!init) {
            logger.info("Setting file loaded successfully.")
        }
    }
}
