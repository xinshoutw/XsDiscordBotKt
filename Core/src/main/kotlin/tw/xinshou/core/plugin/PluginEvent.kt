package tw.xinshou.core.plugin

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.KSerializer
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.core.util.Arguments
import tw.xinshou.core.util.FileGetter
import java.io.File

abstract class PluginEvent(val listener: Boolean) : ListenerAdapter() {
    lateinit var pluginName: String
    open val logger: Logger by lazy { LoggerFactory.getLogger(javaClass) }
    open val fileGetter: FileGetter by lazy { FileGetter(pluginDirectory, javaClass) }
    open val pluginDirectory: File by lazy { File("plugins", pluginName) }
    open val componentPrefix: String by lazy {
        if (pluginName.length > 10) {
            logger.warn("Plugin name is too long, it may cause UID too long.")
        }
        pluginName.lowercase() + "@"
    }

    open fun load() {
        javaClass.getResource("lang")?.let {
            fileGetter.export(resourcePath = "lang/", replace = Arguments.forceRenewLangResources)
        }

        logger.info("{} loaded with default.", pluginName)
    }

    open fun unload() {
        logger.info("{} unloaded with default.", pluginName)
    }

    open fun reload() {
        javaClass.getResource("lang")?.let {
            fileGetter.export(resourcePath = "lang/", replace = Arguments.forceRenewLangResources)
        }
    }

    open fun guildCommands(): Array<CommandData>? = null

    open fun globalCommands(): Array<CommandData>? = null
}

abstract class PluginEventConfigure<C : Any>(
    listener: Boolean,
    private val configSerializer: KSerializer<C>
) : PluginEvent(listener) {
    val config: C by lazy {
        fileGetter.readOrExportInputStream("config.yaml").use { inputStream ->
            Yaml().decodeFromStream(configSerializer, inputStream)
        }
    }
}