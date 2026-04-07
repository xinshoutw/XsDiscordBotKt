# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew                    # Full build (default task), outputs to ./DevServer/
./gradlew build              # Same as above
./gradlew :Core:build        # Build Core only (shadow JAR to DevServer/)
./gradlew :PluginName:build  # Build a single plugin (JAR to DevServer/plugins/)
./gradlew -PoutputPath=/path # Override output directory
```

Build outputs: Core shadow JAR goes to `DevServer/`, plugin JARs go to `DevServer/plugins/`.

Java 21 toolchain is required. Version is composed from root `gradle.properties` (major/minor/snapshot) + module `gradle.properties` (patch).

## Architecture

**Core + Plugin system** for a Discord bot built on JDA (Java Discord API). Core provides the bot framework; plugins are independently built JARs loaded at runtime via reflection.

### Runtime Flow
1. `MainKt.main()` → starts embedded MongoDB (`CacheDbServer`) → `BotLoader.start()`
2. `BotLoader` → `PluginLoader.preLoad()` scans `./plugins/*.jar`, reads `info.yaml`, aggregates JDA intents/cache flags → builds JDA instance
3. `PluginLoader.run()` → calls each plugin's `load()`, collects slash commands → registers with Discord
4. All slash commands receive automatic `deferReply(true)` via `InteractionLogger` — plugins work with deferred replies

### Plugin Contract
- Entry point must be a **Kotlin `object`** (loaded via `INSTANCE` reflection field)
- Extend `PluginEvent` (basic) or `PluginEventConfigure<C>` (with typed config from `config.yaml`)
- Override `load()`, `unload()`, `reload()`, `guildCommands()`, `globalCommands()`
- Override JDA listener methods (`onSlashCommandInteraction`, etc.) to handle events
- Use `GlobalUtil.checkCommandString(event, "command-name")` for command routing

### Plugin Package Convention
- IntelliJ package prefix: `tw.xinshou.discord.plugin` (set by convention plugin)
- Core package prefix: `tw.xinshou.discord`

### Key Resource Files (per plugin)
- `src/main/resources/info.yaml` — plugin metadata, JDA requirements, dependencies
- `src/main/resources/config.yaml` — default config (exported to `plugins/<name>/config.yaml`)
- `src/main/resources/lang/` — i18n files (exported to `plugins/<name>/lang/`)

### Convention Plugins (build-logic)
- `xs-plugin` — standard plugin: `compileOnly(:Core)`, outputs JAR to plugins dir
- `xs-plugin-shadow` — same but uses shadow JAR (for plugins needing bundled runtime deps)
- `xs-jvm-conventions` — base: Java 21 toolchain, version generation, `info.yaml` token expansion (`${author}`, `${name}`, `${coreApi}`, `${version}`)

### Creating a New Plugin
1. Create `Plugins/YourPlugin/` with `build.gradle.kts` applying `id("xs-plugin")` (or `xs-plugin-shadow`)
2. Add `version.patch` to module `gradle.properties`
3. Add module include in root `settings.gradle.kts`
4. Create `info.yaml` with `main` pointing to your Kotlin object
5. If depending on another plugin at runtime, declare both `compileOnly(project(...))` in build and `depend_plugins` in `info.yaml`

### Data Layer
- **Embedded MongoDB** (`CacheDbServer`): `CacheDbManager(pluginName).getCollection(...)` → `MemoryCacheDb` (in-memory + async flush) or `DirectCacheDb`
- **JSON files**: `JsonFileManager<T>` (single file) / `JsonGuildFileManager<T>` (per-guild sharding)
- **Templates**: `MessageCreator` / `ModalCreator` read YAML templates with `Placeholder` substitution
- **Component IDs**: `ComponentIdManager` for reversible component ID encoding/decoding

### Dependencies Catalog
All shared dependency versions are in `gradle/libs.versions.toml`. Key libraries: JDA 6.x, Kotlin 2.2.x, kotlinx-serialization, KAML (YAML), Moshi (JSON), embedded MongoDB, Ktor (web dashboard).
