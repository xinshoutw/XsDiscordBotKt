package tw.xinshou.discord.core.builtin.consolelogger

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.BotLoader.jdaBot
import tw.xinshou.discord.core.base.SettingsLoader
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object ConsoleLogger : ListenerAdapter() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val lock = ReentrantLock()
    private val commandConsoles: MutableList<ConsoleChannel> = ArrayList()
    private val buttonConsoles: MutableList<ConsoleChannel> = ArrayList()
    private val modalConsoles: MutableList<ConsoleChannel> = ArrayList()
    private var listenerRegistered = false

    fun run() = reload()

    fun reload() {
        lock.withLock {
            unregisterListenerIfNeeded()
            commandConsoles.clear()
            buttonConsoles.clear()
            modalConsoles.clear()

            val consoleChannelList = SettingsLoader.config.builtinSettings?.consoleLoggerSetting
            if (consoleChannelList.isNullOrEmpty()) {
                logger.info("No console channel bind")
                return
            }

            consoleChannelList.forEach { consoleData ->
                val guild = jdaBot.getGuildById(consoleData.guildId)
                if (guild == null) {
                    logger.warn("Skipped, Cannot found Guild: {}", consoleData.guildId)
                    return@forEach
                }

                val channel = jdaBot.getTextChannelById(consoleData.channelId)
                if (channel == null) {
                    logger.warn("Skipped, Cannot found Channel: {}", consoleData.channelId)
                    return@forEach
                }

                val console = ConsoleChannel(channel, consoleData.format)
                consoleData.logType.forEach { type ->
                    when (type) {
                        "command" -> commandConsoles.add(console)
                        "button" -> buttonConsoles.add(console)
                        "modal" -> modalConsoles.add(console)
                    }
                }
            }

            if (commandConsoles.isEmpty() && buttonConsoles.isEmpty() && modalConsoles.isEmpty()) {
                logger.info("Console logger has no valid channels after reload.")
                return
            }

            jdaBot.addEventListener(this)
            listenerRegistered = true
            logger.info("Console logger reloaded with {} command, {} button, {} modal targets.",
                commandConsoles.size, buttonConsoles.size, modalConsoles.size)
        }
    }

    fun stop() {
        lock.withLock {
            unregisterListenerIfNeeded()
            commandConsoles.clear()
            buttonConsoles.clear()
            modalConsoles.clear()
        }
    }

    private fun unregisterListenerIfNeeded() {
        if (listenerRegistered) {
            jdaBot.removeEventListener(this)
            listenerRegistered = false
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        commandConsoles.forEach {
            sendChannel(event, it)
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        buttonConsoles.forEach {
            sendChannel(event, it)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        modalConsoles.forEach {
            sendChannel(event, it)
        }
    }

    private fun sendChannel(event: GenericInteractionCreateEvent, console: ConsoleChannel) {
        val interactionString = when (event) {
            is SlashCommandInteractionEvent -> {
                Placeholder.get(event.user)
                    .putAll(
                        "cl_type" to "CMD",
                        "cl_interaction_string" to event.commandString
                    ).parse(console.format)
            }

            is ButtonInteractionEvent -> {
                Placeholder.get(event.user)
                    .putAll(
                        "cl_type" to "BTN",
                        "cl_interaction_string" to event.componentId
                    ).parse(console.format)
            }

            is ModalInteractionEvent -> {
                Placeholder.get(event.user)
                    .putAll(
                        "cl_type" to "BTN",
                        "cl_interaction_string" to event.modalId
                    ).parse(console.format)
            }

            else -> throw IllegalArgumentException("Unsupported event type")
        }

        console.channel.sendMessage(interactionString).queue()
    }

    private data class ConsoleChannel(
        val channel: TextChannel,
        val format: String,
    )
}
