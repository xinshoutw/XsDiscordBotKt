import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val pluginName = "GoogleSheetAPI"
group = "tw.xserver.plugin.api"
version = "0.2.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    api("com.google.api-client:google-api-client:2.7.1")
    api("com.google.oauth-client:google-oauth-client-jetty:1.37.0")
    api("com.google.apis:google-api-services-sheets:v4-rev20250106-2.0.0")
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
