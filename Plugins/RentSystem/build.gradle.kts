import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val pluginName = "RentSystem"
group = "tw.xinshou.plugin"
version = "0.1.0"

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.6"
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.quartz-scheduler:quartz:2.5.0")
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
