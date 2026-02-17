package tw.xinshou.discord.core.plugin

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.KSerializer
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.util.Arguments
import tw.xinshou.discord.core.util.FileGetter
import java.io.File

abstract class PluginEvent(val listener: Boolean) : ListenerAdapter() {
    lateinit var pluginName: String
    open var prefix: String = ""
    open var componentPrefix: String = ""

    open val logger: Logger by lazy { LoggerFactory.getLogger(javaClass) }
    open val fileGetter: FileGetter by lazy { FileGetter(pluginDirectory, javaClass) }
    open val pluginDirectory: File by lazy { File("plugins", pluginName) }

    open fun load() {
        if (hasResource("lang/")) {
            fileGetter.export(resourcePath = "lang/", replace = Arguments.forceRenewLangResources)
        }

        logger.info("{} loaded with default.", pluginName)
    }

    open fun unload() {
        logger.info("{} unloaded with default.", pluginName)
    }

    open fun reload() {
        if (hasResource("lang/")) {
            fileGetter.export(resourcePath = "lang/", replace = Arguments.forceRenewLangResources)
        }
    }

    open fun guildCommands(): Array<CommandData>? = null

    open fun globalCommands(): Array<CommandData>? = null

    private fun hasResource(path: String): Boolean {
        val cleanPath = path.removePrefix("/")
        return javaClass.getResource(cleanPath) != null
            || javaClass.getResource("/$cleanPath") != null
            || javaClass.classLoader.getResource(cleanPath) != null
    }
}

abstract class PluginEventConfigure<C : Any>(
    listener: Boolean,
    private val configSerializer: KSerializer<C>
) : PluginEvent(listener) {
    lateinit var config: C

    override fun load() {
        super.load()

        config = fileGetter.readOrExportInputStream("config.yaml").use { inputStream ->
            Yaml().decodeFromStream(configSerializer, inputStream)
        }
    }

    override fun reload() {
        super.reload()

        config = fileGetter.readOrExportInputStream("config.yaml").use { inputStream ->
            Yaml().decodeFromStream(configSerializer, inputStream)
        }
    }
}
