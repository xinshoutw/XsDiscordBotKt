@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    versionCatalogs { create("libs") {} }
}

rootProject.name = "XsDiscordBotKt"

include(":Plugins:API:GoogleSheetAPI")
project(":Plugins:API:GoogleSheetAPI").name = "GoogleSheetAPI"

include(":Plugins:Economy")
project(":Plugins:Economy").name = "Economy"

include("Plugins:BotInfo")
project(":Plugins:BotInfo").name = "BotInfo"

include("Plugins:ChatLogger")
project(":Plugins:ChatLogger").name = "ChatLogger"

include("Plugins:API:SQLiteAPI")
project(":Plugins:API:SQLiteAPI").name = "SQLiteAPI"

include("Plugins:IntervalPusher")
project(":Plugins:IntervalPusher").name = "IntervalPusher"

include("Plugins:TicketAddons")
project(":Plugins:TicketAddons").name = "TicketAddons"

include("Plugins:VoiceLogger")
project(":Plugins:VoiceLogger").name = "VoiceLogger"

include("Plugins:DynamicVoiceChannel")
project(":Plugins:DynamicVoiceChannel").name = "DynamicVoiceChannel"

include("Plugins:Feedbacker")
project(":Plugins:Feedbacker").name = "Feedbacker"

include("Plugins:BasicCalculator")
project(":Plugins:BasicCalculator").name = "BasicCalculator"

include("Plugins:Ticket")
project(":Plugins:Ticket").name = "Ticket"

include("Plugins:AutoRole")
project(":Plugins:AutoRole").name = "AutoRole"

include("Plugins:RentSystem")
project(":Plugins:RentSystem").name = "RentSystem"

include("Plugins:MusicPlayer")
project(":Plugins:MusicPlayer").name = "MusicPlayer"

include("Plugins:API:AudioAPI")
project(":Plugins:API:AudioAPI").name = "AudioAPI"

include("Plugins:SimpleCommand")
project(":Plugins:SimpleCommand").name = "SimpleCommand"