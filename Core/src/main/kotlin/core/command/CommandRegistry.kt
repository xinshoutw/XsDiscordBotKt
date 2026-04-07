package core.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.LoggerFactory

class CommandRegistry {
    private val logger = LoggerFactory.getLogger(CommandRegistry::class.java)
    private val handlers = mutableMapOf<String, suspend (SlashCommandInteractionEvent) -> Unit>()
    private val commandDataList = mutableListOf<CommandHandler>()
    private val pluginCommandMap = mutableMapOf<String, MutableList<String>>()

    fun register(handler: CommandHandler, pluginName: String? = null) {
        val name = handler.data.name
        commandDataList.add(handler)
        handlers[name] = handler.execute
        logger.debug("Registered command: {}", name)

        if (pluginName != null) {
            pluginCommandMap.getOrPut(pluginName) { mutableListOf() }.add(name)
        }
    }

    fun deregisterAll(pluginName: String) {
        val commands = pluginCommandMap.remove(pluginName) ?: return
        for (cmd in commands) {
            handlers.remove(cmd)
            commandDataList.removeAll { it.data.name == cmd }
        }
        logger.debug("Deregistered all commands for plugin: {}", pluginName)
    }

    suspend fun dispatch(event: SlashCommandInteractionEvent) {
        val handler = handlers[event.fullCommandName]
        if (handler != null) {
            handler.invoke(event)
        } else {
            logger.warn("No handler found for command: {}", event.fullCommandName)
        }
    }

    val guildCommands: List<CommandData>
        get() = commandDataList.filter { !it.isGlobal }.map { it.data }

    val globalCommands: List<CommandData>
        get() = commandDataList.filter { it.isGlobal }.map { it.data }

    fun clear() {
        handlers.clear()
        commandDataList.clear()
        pluginCommandMap.clear()
    }
}
