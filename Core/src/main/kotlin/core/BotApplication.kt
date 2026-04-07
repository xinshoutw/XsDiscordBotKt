package core

import core.builtin.*
import core.command.*
import core.config.BotConfig
import core.logger.InteractionLogger
import core.plugin.PluginRegistry
import dev.minn.jda.ktx.jdabuilder.light
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.cache.CacheFlag

class BotApplication(
    private val config: BotConfig,
    private val pluginRegistry: PluginRegistry,
    private val commandRegistry: CommandRegistry,
    private val componentRegistry: ComponentRegistry,
    private val interactionLogger: InteractionLogger,
) {
    lateinit var jda: JDA
        private set

    private var statusChanger: StatusChanger? = null
    private var consoleLogger: ConsoleLogger? = null
    private var appEmoji: AppEmoji? = null

    suspend fun start() {
        // 1. Discover and load plugins (collects intents, cache flags)
        pluginRegistry.discoverAndLoad()

        // 2. Build JDA with aggregated requirements from plugins
        jda = light(config.botToken, enableCoroutines = true) {
            enableIntents(pluginRegistry.aggregateIntents())
            enableCache(pluginRegistry.aggregateCacheFlags())
            setMemberCachePolicy(pluginRegistry.aggregateMemberCachePolicy())
            // Disable unused caches
            disableCache(
                CacheFlag.ACTIVITY,
                CacheFlag.CLIENT_STATUS,
            )
            setBulkDeleteSplittingEnabled(false)
        }

        // 3. Register central event listener
        jda.addEventListener(interactionLogger)
        jda.addEventListener(CoreEventListener(commandRegistry, componentRegistry))

        // 4. Wait for JDA ready
        jda.awaitReady()

        // 5. Register commands
        refreshCommands()

        // 6. Start builtin features
        statusChanger = StatusChanger(jda, config.statusChanger)
        statusChanger?.start(CoroutineScope(Dispatchers.Default))

        consoleLogger = ConsoleLogger(jda, config.consoleLoggers)
        appEmoji = AppEmoji(jda).also { it.initialize() }
    }

    suspend fun stop() {
        statusChanger?.stop()
        pluginRegistry.unloadAll()
        if (::jda.isInitialized) {
            jda.shutdown()
            // Await with timeout
            withTimeoutOrNull(10_000) {
                while (!jda.status.isShutdown) {
                    delay(100)
                }
            }
        }
    }

    suspend fun reload() {
        stop()
        commandRegistry.clear()
        componentRegistry.clear()
        start()
    }

    private fun refreshCommands() {
        // Register global commands
        jda.updateCommands().addCommands(commandRegistry.globalCommands).queue()

        // Register guild commands to all guilds
        for (guild in jda.guilds) {
            guild.updateCommands().addCommands(commandRegistry.guildCommands).queue()
        }
    }
}

// Internal: Central event listener that dispatches to registries
private class CoreEventListener(
    private val commandRegistry: CommandRegistry,
    private val componentRegistry: ComponentRegistry,
) : ListenerAdapter() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        scope.launch { commandRegistry.dispatch(event) }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        scope.launch {
            val handler = componentRegistry.findHandler(event.componentId)
            handler?.onButton?.invoke(event)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        scope.launch {
            val handler = componentRegistry.findHandler(event.modalId)
            handler?.onModal?.invoke(event)
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        scope.launch {
            val handler = componentRegistry.findHandler(event.componentId)
            handler?.onStringSelect?.invoke(event)
        }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        scope.launch {
            val handler = componentRegistry.findHandler(event.componentId)
            handler?.onEntitySelect?.invoke(event)
        }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        // Register guild commands for new guilds
        event.guild.updateCommands().addCommands(commandRegistry.guildCommands).queue()
    }
}
