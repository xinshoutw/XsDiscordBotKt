package core.di

import core.builtin.*
import core.command.*
import core.config.*
import core.dashboard.DashboardServer
import core.database.DatabaseProvider
import core.logger.InteractionLogger
import core.plugin.PluginRegistry
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
