val pluginName = "Economy"
group = "tw.xinshou.plugin"
version = "0.2.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

dependencies {
    compileOnly(project(":Plugins:API:GoogleSheetAPI"))
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
