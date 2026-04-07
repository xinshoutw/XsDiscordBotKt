package core.plugin

import core.command.CommandHandler
import core.command.ComponentHandler
import org.koin.core.module.Module

interface Plugin {
    var config: PluginConfig

    fun Module.definitions() {}

    fun PluginContext.onLoad() {}
    fun PluginContext.onUnload() {}
    fun PluginContext.onReload() {
        onUnload()
        onLoad()
    }

    fun commands(): List<CommandHandler> = emptyList()
    fun components(): List<ComponentHandler> = emptyList()

    /**
     * Return JDA event listeners that should be registered after JDA is ready.
     * Use this for plugins that need to listen to raw JDA events
     * (e.g., GuildMemberJoinEvent, MessageReceivedEvent).
     */
    fun listeners(): List<Any> = emptyList()
}
