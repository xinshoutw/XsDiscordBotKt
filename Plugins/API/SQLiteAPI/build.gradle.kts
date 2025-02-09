import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val pluginName = "SQLiteAPI"
group = "tw.xinshou.api"
version = "0.2.0"

plugins {
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    api("org.xerial:sqlite-jdbc:3.49.0.0")
    api("org.jetbrains.exposed:exposed-core:0.59.0")
    api("org.jetbrains.exposed:exposed-dao:0.59.0")
    api("org.jetbrains.exposed:exposed-jdbc:0.59.0")
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
