package tw.xinshou.plugin.autorole

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.plugin.PluginEvent
import tw.xinshou.loader.util.FileGetter
import tw.xinshou.plugin.autorole.serializer.MainConfigSerializer
import java.io.File
import java.io.IOException


/**
 * Main class for the Economy plugin managing configurations, commands, and data handling.
 */
object Event : PluginEvent(true) {
    private val PLUGIN_DIR_FILE = File("plugins/AutoRole")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    internal lateinit var config: MainConfigSerializer

    override fun load() {
        fileGetter = FileGetter(PLUGIN_DIR_FILE, this::class.java)
        reloadAll()

        logger.info("AutoRole loaded.")
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

    override fun unload() {
        logger.info("AutoRole unloaded.")
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        AutoRole.onGuildMemberJoin(event)
    }
}
