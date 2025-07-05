import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val pluginName = "GoogleSheetAPI"
group = "tw.xinshou.plugin.api"
version = "0.2.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    api("com.google.api-client:google-api-client:2.8.0")
    api("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    api("com.google.apis:google-api-services-sheets:v4-rev20250616-2.0.0")
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
