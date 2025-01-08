package tw.xserver.loader.plugin

import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xserver.loader.util.FileGetter

abstract class PluginEvent(val listener: Boolean) : ListenerAdapter() {
    open lateinit var fileGetter: FileGetter
    lateinit var pluginName: String

    abstract fun load()
    abstract fun unload()
    open fun reloadConfigFile() {}
    open fun reloadLang() {}

    open fun reloadAll() {
        reloadConfigFile()
        reloadLang()
    }

    open fun guildCommands(): Array<CommandData>? = null

    open fun globalCommands(): Array<CommandData>? = null
}
