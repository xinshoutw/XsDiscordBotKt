rootProject.name = "XsDiscordBotKt"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":Plugins:API:GoogleSheetAPI")
findProject(":Plugins:API:GoogleSheetAPI")?.name = "GoogleSheetAPI"

include(":Plugins:Economy")
findProject(":Plugins:Economy")?.name = "Economy"

include("Plugins:BotInfo")
findProject(":Plugins:BotInfo")?.name = "BotInfo"

include("Plugins:ChatLogger")
findProject(":Plugins:ChatLogger")?.name = "ChatLogger"

include("Plugins:ChatLogger")
findProject(":Plugins:ChatLogger")?.name = "ChatLogger"

include("Plugins:API:SQLiteAPI")
findProject(":Plugins:API:SQLiteAPI")?.name = "SQLiteAPI"

include("Plugins:IntervalPusher")
findProject(":Plugins:IntervalPusher")?.name = "IntervalPusher"

include("Plugins:TicketAddons")
findProject(":Plugins:TicketAddons")?.name = "TicketAddons"

include("Plugins:VoiceLogger")
findProject(":Plugins:VoiceLogger")?.name = "VoiceLogger"

include("Plugins:DynamicVoiceChannel")
findProject(":Plugins:DynamicVoiceChannel")?.name = "DynamicVoiceChannel"

include("Plugins:Feedbacker")
findProject(":Plugins:Feedbacker")?.name = "Feedbacker"

include("Plugins:BasicCalculator")
findProject(":Plugins:BasicCalculator")?.name = "BasicCalculator"

include("Plugins:Ticket")
findProject(":Plugins:Ticket")?.name = "Ticket"

include("Plugins:AutoRole")
findProject(":Plugins:AutoRole")?.name = "AutoRole"
