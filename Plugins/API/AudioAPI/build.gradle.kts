import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val pluginName = "AudioAPI"
group = "tw.xinshou.plugin.api"
version = "0.1.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    api("club.minnced:opus-java:1.1.1")
    api("com.google.crypto.tink:tink:1.17.0")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named<ShadowJar>("shadowJar") {
    val outputPath: File by rootProject.extra
    archiveBaseName = pluginName
    archiveAppendix = "${properties["prefix"]}"
    archiveVersion = "$version"
    archiveClassifier = ""
    archiveExtension = "jar"
    destinationDirectory = outputPath.resolve("plugins")
}
