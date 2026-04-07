package core.plugin

import core.command.CommandHandler
import core.command.ComponentHandler
import org.koin.core.module.Module

interface Plugin {
    val config: PluginConfig

    fun Module.definitions() {}

    fun PluginContext.onLoad() {}
    fun PluginContext.onUnload() {}
    fun PluginContext.onReload() {
        onUnload()
        onLoad()
    }

    fun commands(): List<CommandHandler> = emptyList()
    fun components(): List<ComponentHandler> = emptyList()
}
