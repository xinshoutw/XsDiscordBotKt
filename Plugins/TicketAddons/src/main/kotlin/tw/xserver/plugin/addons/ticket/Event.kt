package tw.xserver.plugin.addons.ticket

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.plugin.PluginEvent
import tw.xserver.loader.util.FileGetter
import tw.xserver.plugin.addons.ticket.serializer.MainConfigSerializer
import java.io.File
import java.io.IOException

object Event : PluginEvent(true) {
    internal val PLUGIN_DIR_FILE = File("plugins/TicketAddons")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal lateinit var config: MainConfigSerializer
        private set

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadAll()

        logger.info("TicketAddons loaded.")
    }

    override fun unload() {
        logger.info("TicketAddons unloaded.")
    }

    override fun reloadConfigFile() {

        try {
            fileGetter.readInputStream("config.yaml").use {
                config = Yaml().decodeFromStream<MainConfigSerializer>(it)
            }
        } catch (e: IOException) {
            logger.error("Please configure {}./config.yaml!", PLUGIN_DIR_FILE.canonicalPath, e)
        }

        logger.info("Setting file loaded successfully.")
    }

    override fun reloadLang() {
        fileGetter.exportDefaultDirectory("lang")
    }

    override fun onChannelCreate(event: ChannelCreateEvent) {
        TicketAddons.onChannelCreate(event)
    }
}
