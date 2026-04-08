package tw.xinshou.discord.core.plugin

import tw.xinshou.discord.core.util.FileGetter
import org.slf4j.Logger
import java.io.File

class PluginContext(
    val pluginName: String,
    val pluginDirectory: File,
    val logger: Logger,
) {
    val fileGetter: FileGetter by lazy {
        FileGetter(pluginDirectory, this::class.java)
    }
}
