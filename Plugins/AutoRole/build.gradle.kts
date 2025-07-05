val pluginName = "AutoRole"
group = "tw.xinshou.plugin"
version = "0.1.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

tasks.named<Jar>("jar") {
    val outputPath: File by rootProject.extra

    archiveBaseName = pluginName
    archiveAppendix = "${properties["prefix"]}"
    archiveVersion = "$version"
    archiveClassifier = ""
    archiveExtension = "jar"
    destinationDirectory = outputPath.resolve("plugins")
}
