@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic/convention")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

include("Core")
project(":Core").name = "Core"

include("Plugins:_Example")
project(":Plugins:_Example").name = "_Example"

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

include("Plugins:MusicPlayer")
project(":Plugins:MusicPlayer").name = "MusicPlayer"

include("Plugins:API:AudioAPI")
project(":Plugins:API:AudioAPI").name = "AudioAPI"

include("Plugins:SimpleCommand")
project(":Plugins:SimpleCommand").name = "SimpleCommand"

include("Plugins:Giveaway")
project(":Plugins:Giveaway").name = "Giveaway"

include("Plugins:NtustManager")
project(":Plugins:NtustManager").name = "NtustManager"

include("Plugins:NtustCourse")
project(":Plugins:NtustCourse").name = "NtustCourse"

include("WebDashboard")
project(":WebDashboard").projectDir = file("Web-Dashboard")
project(":WebDashboard").name = "WebDashboard"
