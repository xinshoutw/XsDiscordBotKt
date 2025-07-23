package tw.xinshou.plugin.addons.ticket

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.plugin.addons.ticket.serializer.MainConfigSerializer
import java.io.File
import java.io.IOException

object Event : PluginEvent(true) {
    internal val PLUGIN_DIR_FILE = File("plugins/TicketAddons")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal lateinit var config: MainConfigSerializer
        private set

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reload(true)

        logger.info("TicketAddons loaded.")
    }

    override fun unload() {
        logger.info("TicketAddons unloaded.")
    }

    override fun reload(init: Boolean) {
        fileGetter.exportDefaultDirectory("lang")

        try {
            fileGetter.readInputStream("config.yaml").use {
                config = Yaml().decodeFromStream<MainConfigSerializer>(it)
            }
        } catch (e: IOException) {
            logger.error("Please configure {}./config.yaml!", PLUGIN_DIR_FILE.canonicalPath, e)
        }

        logger.info("Setting file loaded successfully.")
    }

    override fun onChannelCreate(event: ChannelCreateEvent) {
        TicketAddons.onChannelCreate(event)
    }
}
