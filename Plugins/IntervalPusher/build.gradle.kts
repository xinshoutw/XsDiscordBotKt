val pluginName = "IntervalPusher"
group = "tw.xserver.plugin"
version = "v1.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
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