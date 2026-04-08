package tw.xinshou.discord.core

import tw.xinshou.discord.core.builtin.*
import tw.xinshou.discord.core.command.*
import tw.xinshou.discord.core.config.BotConfig
import tw.xinshou.discord.core.logger.InteractionLogger
import tw.xinshou.discord.core.plugin.PluginRegistry
import dev.minn.jda.ktx.jdabuilder.light
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
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

    private var jdaModule: org.koin.core.module.Module? = null
    private var eventListener: CoreEventListener? = null
    private val builtinScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
        eventListener = CoreEventListener(commandRegistry, componentRegistry)
        jda.addEventListener(eventListener)

        // 3.5 Register plugin-provided JDA event listeners
        for (listener in pluginRegistry.aggregateListeners()) {
            jda.addEventListener(listener)
        }

        // 3.6 Register JDA in Koin so plugins can access it at runtime
        jdaModule = module { single<JDA> { jda } }.also { loadKoinModules(it) }

        // 4. Wait for JDA ready
        jda.awaitReady()

        // 5. Register commands
        refreshCommands()

        // 6. Start builtin features
        statusChanger = StatusChanger(jda, config.statusChanger)
        statusChanger?.start(builtinScope)

        consoleLogger = ConsoleLogger(jda, config.consoleLoggers)
        appEmoji = AppEmoji(jda).also { it.initialize() }
    }

    suspend fun stop() {
        statusChanger?.stop()
        eventListener?.cancel()
        builtinScope.cancel()
        pluginRegistry.unloadAll()
        jdaModule?.let { unloadKoinModules(it) }
        jdaModule = null
        if (::jda.isInitialized) {
            jda.shutdown()
            withTimeoutOrNull(10_000) {
                while (jda.status != JDA.Status.SHUTDOWN) {
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

private class CoreEventListener(
    private val commandRegistry: CommandRegistry,
    private val componentRegistry: ComponentRegistry,
) : ListenerAdapter() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun cancel() = scope.cancel()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        scope.launch { commandRegistry.dispatch(event) }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        dispatchComponent(event.componentId) { it.onButton?.invoke(event) }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        dispatchComponent(event.modalId) { it.onModal?.invoke(event) }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        dispatchComponent(event.componentId) { it.onStringSelect?.invoke(event) }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        dispatchComponent(event.componentId) { it.onEntitySelect?.invoke(event) }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        event.guild.updateCommands().addCommands(commandRegistry.guildCommands).queue()
    }

    private fun dispatchComponent(id: String, action: suspend (ComponentHandler) -> Unit) {
        val handler = componentRegistry.findHandler(id) ?: return
        scope.launch { action(handler) }
    }
}
