val pluginName = "DynamicVoiceChannel"
group = "tw.xserver.plugin"
version = "v2.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}


dependencies {
    compileOnly(project(":Plugins:MessageCreator"))
}

//tasks.named<Jar>("jar") {
//    val outputPath: File by rootProject.extra
//
//    archiveBaseName = pluginName
//    archiveAppendix = "${properties["prefix"]}"
//    archiveVersion = "$version"
//    archiveClassifier = ""
//    archiveExtension = "jar"
//    destinationDirectory = outputPath.resolve("plugins")
//}
