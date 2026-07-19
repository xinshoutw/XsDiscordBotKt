package tw.xinshou.discord.core.di

import tw.xinshou.discord.core.builtin.*
import tw.xinshou.discord.core.command.*
import tw.xinshou.discord.core.config.*
import tw.xinshou.discord.core.dashboard.DashboardServer
import tw.xinshou.discord.core.database.DatabaseProvider
import tw.xinshou.discord.core.logger.InteractionLogger
import tw.xinshou.discord.core.plugin.PluginRegistry
import org.koin.core.module.dsl.*
import org.koin.dsl.module
import java.io.File

val coreModule = module {
    // Config
    single {
        val configFile = File("config.yaml")
        ConfigLoader.load<BotConfig>(configFile, "config.yaml")
    }

    // Database
    single { DatabaseProvider(get<BotConfig>().database) }

    // Registries
    single { CommandRegistry() }
    single { ComponentRegistry() }
    single { PluginRegistry(File("plugins"), get(), get()) }

    // Logger
    single { InteractionLogger() }

    // Dashboard
    single { DashboardServer() }
}
