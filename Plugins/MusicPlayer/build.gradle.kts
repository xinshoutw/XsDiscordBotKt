import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val pluginName = "MusicPlayer"
group = "tw.xinshou.plugin"
version = "0.1.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.6"
}

repositories {
    maven(url = "https://maven.lavalink.dev/releases")
    maven(url = "https://maven.topi.wtf/releases")
    maven(url = "https://jitpack.io")
}

dependencies {
    compileOnly(project(":Plugins:API:AudioAPI"))
    implementation("com.github.topi314.lavasrc:lavasrc:4.7.2")
    implementation("com.github.topi314.lavasrc:lavasrc-protocol:4.7.2")
    implementation("dev.arbjerg:lavaplayer:2.2.4")
    implementation("dev.lavalink.youtube:common:1.13.3")
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
