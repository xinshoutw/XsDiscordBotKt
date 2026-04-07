# XsDiscordBotKt v4.0.0 - Full Rewrite Design Spec

**Date**: 2026-04-07
**Target Version**: 4.0.0
**Branch**: `refactor/modern-change`
**Scope**: Core + all Plugins + build system (Dashboard deferred)

---

## 1. Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| DI Framework | Koin | Kotlin-first, Ktor native integration, module isolation for hot-reload |
| Serialization | kotlinx-serialization only | Unified `@Serializable` for YAML (KAML), JSON, MongoDB codec |
| Database | MongoDB coroutine driver + rewritten wrapper | Keep document model, add external connection support |
| Plugin System | Koin module-based | No reflection, type-safe, natural hot-reload via loadKoinModules/unloadKoinModules |
| Command Routing | Registration-based map dispatch | O(1) lookup, no broadcast, testable handlers |
| JDA Extensions | jda-ktx | `await()` coroutine bridge, Kotlin-idiomatic API |
| Web Dashboard | Deferred (skeleton only) | Focus on Core + Plugins stability first |

## 2. Dependency Upgrades

| Dependency | v3 (current) | v4 (target) |
|-----------|-------------|-------------|
| Kotlin | 2.2.21 | 2.3.10 |
| JDA | 6.4.1 | 6.4.1 (keep) |
| jda-ktx | N/A | latest for JDA 6 |
| Koin | N/A | latest stable |
| kotlinx-serialization | 1.9.0 | keep or bump |
| kotlinx-coroutines | 1.10.2 | keep |
| MongoDB driver | 5.6.2 (sync) | kotlin-coroutine driver |
| Moshi | 1.x | **REMOVE** |

## 3. Core Module Architecture

### 3.1 Package Structure

```
core/
├── Main.kt                          # Entry point, startKoin {}, runBlocking
├── BotApplication.kt                # JDA lifecycle management (injectable)
├── config/
│   ├── BotConfig.kt                 # @Serializable bot configuration
│   └── ConfigLoader.kt              # YAML config loading utility
├── di/
│   └── CoreModule.kt                # Koin module: BotApplication, DatabaseProvider, etc.
├── plugin/
│   ├── Plugin.kt                    # Plugin interface
│   ├── PluginConfig.kt              # @Serializable plugin metadata (from info.yaml)
│   ├── PluginRegistry.kt            # Discovery, dependency sort, lifecycle
│   └── PluginContext.kt             # API surface given to plugins (inject, register, i18n)
├── command/
│   ├── CommandRegistry.kt           # Map<String, CommandHandler> dispatch
│   ├── CommandHandler.kt            # Sealed interface: SlashCommand, SubCommand
│   └── ComponentRegistry.kt         # Button/modal/select handler dispatch by prefix
├── database/
│   ├── DatabaseProvider.kt          # MongoDB connection (embedded or external)
│   ├── DatabaseConfig.kt            # @Serializable connection config
│   ├── Collection.kt                # Type-safe suspend CRUD wrapper
│   └── GuildCollection.kt           # Per-guild sharded collection
├── i18n/
│   ├── Localizer.kt                 # Language file loading + fallback resolution
│   ├── LocaleMap.kt                 # Map<DiscordLocale, String> with fallback
│   └── MessageTemplate.kt           # YAML-based message/modal builder (merged v1+v2)
├── placeholder/
│   ├── Placeholder.kt               # Extension functions for User/Member/Guild/Event
│   └── Substitutor.kt               # %key% replacement engine
├── logger/
│   ├── LogbackConfig.kt             # Logback programmatic configuration
│   └── InteractionLogger.kt         # Auto defer + logging (simplified)
├── cli/
│   └── ConsoleManager.kt            # JLine REPL
└── util/
    ├── ComponentId.kt               # Encode/decode component IDs (keep design)
    └── JdaExtensions.kt             # JDA Kotlin extensions (complement jda-ktx)
```

### 3.2 Removed from Core

| Removed | Reason |
|---------|--------|
| `PluginSandboxClassLoader` | Monorepo, no isolation needed |
| `MessageCreator` v1 | Merged into single `MessageTemplate` |
| `GlobalUtil` object | Split into extension functions |
| `JsonFileManager` / `JsonGuildFileManager` (Moshi) | Replaced by kotlinx-serialization utilities |
| `CacheCollectionManager` / `MemoryCacheDb` / `DirectCacheDb` | Replaced by `Collection<T>` with coroutine driver |
| `TypedListWrapper` | Eliminated by kotlinx-serialization codec |
| `UpdateChecker` (dead code) | Remove entirely |
| `SettingsLoader` | Replaced by `ConfigLoader` using kotlinx-serialization |

### 3.3 Entry Point (Main.kt)

```kotlin
fun main(args: Array<String>) = runBlocking {
    // 1. Parse CLI args
    Arguments.parse(args)

    // 2. Start Koin
    startKoin {
        modules(coreModule)
    }

    // 3. Start services (injected)
    val db: DatabaseProvider by inject()
    val bot: BotApplication by inject()
    val console: ConsoleManager by inject()

    db.start()
    bot.start()       // loads plugins, builds JDA, registers commands
    console.start(scope = this, stopSignal = stopSignal)

    stopSignal.await()

    bot.stop()
    db.stop()
    console.stop()
}
```

### 3.4 BotApplication (replaces BotLoader)

```kotlin
class BotApplication(
    private val config: BotConfig,
    private val pluginRegistry: PluginRegistry,
    private val commandRegistry: CommandRegistry,
    private val componentRegistry: ComponentRegistry,
) {
    lateinit var jda: JDA
        private set

    suspend fun start() {
        pluginRegistry.discoverAndLoad()

        jda = light(config.token) {
            // Aggregate intents/cache from plugin configs
            val intents = pluginRegistry.aggregateIntents()
            enableIntents(intents)
            // ... JDA builder config
        }

        jda.awaitReady()

        // Register commands from all plugins
        commandRegistry.registerAll(jda)
    }

    suspend fun stop() {
        pluginRegistry.unloadAll()
        jda.shutdown()
        jda.awaitShutdown()
    }

    suspend fun reload() {
        stop()
        start()
    }
}
```

## 4. Plugin System

### 4.1 Plugin Interface

```kotlin
interface Plugin {
    /** Plugin metadata loaded from info.yaml */
    val config: PluginConfig

    /** Koin dependency definitions for this plugin */
    fun Module.definitions() {}

    /** Called after Koin module installed, JDA ready */
    fun PluginContext.onLoad() {}

    /** Called during shutdown */
    fun PluginContext.onUnload() {}

    /** Called during hot-reload */
    fun PluginContext.onReload() {
        onUnload()
        onLoad()
    }

    /** Slash command handlers */
    fun commands(): List<CommandHandler> = emptyList()

    /** Component interaction handlers (buttons, modals, selects) */
    fun components(): List<ComponentHandler> = emptyList()
}
```

### 4.2 PluginConfig (info.yaml)

```kotlin
@Serializable
data class PluginConfig(
    val name: String,
    val version: String,
    val author: String,
    val coreApi: String,
    val main: String,
    val componentPrefix: String = "",
    val requireIntents: List<String> = emptyList(),
    val requireCacheFlags: List<String> = emptyList(),
    val requireMemberCachePolicies: List<String> = emptyList(),
    val dependPlugins: List<String> = emptyList(),
    val softDependPlugins: List<String> = emptyList(),
)
```

### 4.3 PluginRegistry Lifecycle

```
discoverAndLoad():
  1. Scan plugins/ for JARs
  2. Read info.yaml → PluginConfig (kotlinx-serialization + KAML)
  3. Validate coreApi compatibility
  4. Topological sort by dependPlugins
  5. For each plugin (in order):
     a. Instantiate via ServiceLoader (META-INF/services) or Koin
     b. Install Koin module: loadKoinModules(plugin.definitions())
     c. Call plugin.onLoad()
     d. Register plugin.commands() → CommandRegistry
     e. Register plugin.components() → ComponentRegistry

unloadAll():
  1. For each plugin (reverse order):
     a. Call plugin.onUnload()
     b. Unload Koin module: unloadKoinModules(...)
     c. Deregister commands and components
```

### 4.4 Plugin Example (Economy)

```kotlin
class EconomyPlugin : Plugin {
    override val config = pluginConfig("Economy")

    override fun Module.definitions() {
        single { EconomyConfig.load(get()) }
        single { EconomyService(get(), get()) }
    }

    override fun PluginContext.onLoad() {
        logger.info("Economy plugin loaded")
    }

    override fun commands() = listOf(
        slashCommand("economy", "balance") { event ->
            val service: EconomyService by inject()
            service.showBalance(event)
        },
        slashCommand("economy", "transfer") { event ->
            val service: EconomyService by inject()
            service.transfer(event)
        },
    )

    override fun components() = listOf(
        button("economy@") { event ->
            val service: EconomyService by inject()
            service.handleButton(event)
        },
    )
}
```

## 5. Command Routing

### 5.1 CommandRegistry

```kotlin
class CommandRegistry {
    private val slashHandlers = mutableMapOf<String, suspend (SlashCommandInteractionEvent) -> Unit>()

    fun register(handler: CommandHandler) {
        val key = handler.fullCommandName  // e.g. "economy balance"
        slashHandlers[key] = handler.execute
    }

    suspend fun dispatch(event: SlashCommandInteractionEvent) {
        val handler = slashHandlers[event.fullCommandName]
        if (handler != null) {
            handler(event)
        } else {
            event.reply("Unknown command").setEphemeral(true).await()
        }
    }
}
```

### 5.2 ComponentRegistry

```kotlin
class ComponentRegistry {
    private val handlers = mutableMapOf<String, suspend (GenericComponentInteractionCreateEvent) -> Unit>()

    fun register(prefix: String, handler: suspend (GenericComponentInteractionCreateEvent) -> Unit) {
        handlers[prefix] = handler
    }

    suspend fun dispatch(event: GenericComponentInteractionCreateEvent) {
        val id = event.componentId
        val handler = handlers.entries.find { id.startsWith(it.key) }?.value
        handler?.invoke(event)
    }
}
```

### 5.3 Central Event Listener

```kotlin
class CoreEventListener(
    private val commandRegistry: CommandRegistry,
    private val componentRegistry: ComponentRegistry,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()
        scope.launch {
            commandRegistry.dispatch(event)
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        scope.launch { componentRegistry.dispatch(event) }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        scope.launch { componentRegistry.dispatch(event) }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        scope.launch { componentRegistry.dispatch(event) }
    }
}
```

## 6. Database Layer

### 6.1 DatabaseProvider

```kotlin
class DatabaseProvider(private val config: DatabaseConfig) {
    private var embeddedServer: MongodExecutable? = null
    lateinit var client: MongoClient
        private set

    suspend fun start() {
        if (config.embedded) {
            embeddedServer = startEmbeddedMongo(config)
        }
        client = MongoClient.create(config.connectionString)
    }

    fun database(name: String): MongoDatabase = client.getDatabase(name)

    suspend fun stop() {
        client.close()
        embeddedServer?.stop()
    }
}
```

### 6.2 Collection<T>

```kotlin
class Collection<T : Any>(
    private val collection: MongoCollection<T>,
) {
    suspend fun get(key: String): T? =
        collection.find(eq("_id", key)).firstOrNull()

    suspend fun upsert(key: String, value: T) {
        collection.replaceOne(eq("_id", key), value, ReplaceOptions().upsert(true))
    }

    suspend fun delete(key: String) {
        collection.deleteOne(eq("_id", key))
    }

    suspend fun findAll(): List<T> =
        collection.find().toList()

    suspend fun count(): Long =
        collection.countDocuments()
}
```

All operations are `suspend` using the MongoDB Kotlin Coroutine Driver. No more `MemoryCacheDb` vs `DirectCacheDb` split - caching is the application's concern, not the DB layer's.

### 6.3 DatabaseConfig

```kotlin
@Serializable
data class DatabaseConfig(
    val embedded: Boolean = true,
    val connectionString: String = "mongodb://localhost:27017",
    val port: Int = 27017,
    val dataPath: String = "mongodb-data",
)
```

## 7. Internationalization

### 7.1 Localizer

```kotlin
class Localizer(
    private val pluginDir: File,
    private val defaultLocale: DiscordLocale,
) {
    private val strings: Map<String, LocaleMap> = loadFromYaml()

    operator fun get(key: String): LocaleMap =
        strings[key] ?: error("Missing i18n key: $key")

    fun get(key: String, locale: DiscordLocale): String =
        strings[key]?.resolve(locale, defaultLocale) ?: "[$key]"
}
```

Fails explicitly on missing keys instead of `!!` NPE.

### 7.2 MessageTemplate (merged v1 + v2)

Single implementation replacing both `MessageCreator` v1 and v2. Loads from `lang/{locale}/message/*.yml`, supports:
- Embeds, action rows (buttons, selects)
- Placeholder substitution
- Locale fallback chain
- Modal templates

## 8. Placeholder System

Rewritten as extension functions:

```kotlin
// Usage
val sub = Substitutor()
    .withUser(event.user)
    .withMember(event.member)
    .withGuild(event.guild)
    .withCommand(event)

val message = sub.parse("Hello %user_name%, welcome to %guild_name%!")
```

Builder pattern instead of static `Placeholder.get()` methods.

## 9. Build System Changes

### 9.1 Version Bump

```properties
# gradle.properties (root)
version.major=4
version.minor=0
version.snapshot=true
```

### 9.2 libs.versions.toml Updates

- Add: `koin-core`, `koin-ktor`, `jda-ktx`
- Remove: `moshi-kotlin`
- Bump: `kotlin` → 2.3.10
- Change: `mongodb-driver-kotlin-sync` → `mongodb-driver-kotlin-coroutine`

### 9.3 Convention Plugins

- `xs-jvm-conventions`: Kotlin 2.3.10, Java 25 (keep)
- `xs-plugin`: Add Koin as compileOnly dependency
- `xs-plugin-shadow`: Same
- Package prefix stays `tw.xinshou.discord.plugin`

## 10. Migration Checklist (per plugin)

For each of the 22 plugins:

1. [ ] Replace `object Event : PluginEvent(listener)` → `class XxxPlugin : Plugin`
2. [ ] Replace manual `load()`/`reload()` → Koin `Module.definitions()` + `onLoad()`
3. [ ] Replace `GlobalUtil.checkCommandString()` → `commands()` list
4. [ ] Replace Moshi `@Json` → `@Serializable`
5. [ ] Replace `CacheDbManager` → injected `DatabaseProvider.collection<T>()`
6. [ ] Replace `MessageCreator` v1/v2 → `MessageTemplate`
7. [ ] Replace `.complete()` → `.await()` (jda-ktx)
8. [ ] Replace `Placeholder.get()` → `Substitutor().withUser().withGuild()`
9. [ ] Update `info.yaml` if needed

## 11. What Is NOT Changing

- `info.yaml` as plugin metadata format (but parsed with kotlinx-serialization)
- `ComponentIdManager` encoding scheme (prefix + single-char keys)
- YAML-based message/modal templates (format stays, loader rewritten)
- `lang/` directory structure for i18n
- Build output to `DevServer/` and `DevServer/plugins/`
- Gradle convention plugin structure (`build-logic/convention`)
- Web Dashboard frontend (React + Tailwind, untouched)

## 12. Out of Scope

- Dashboard frontend rewrite
- New features (v4.0.0 is architecture-only)
- Plugin business logic changes (only API adaptation)
- CI/CD pipeline
- Docker / deployment configuration
