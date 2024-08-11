import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20-RC"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "tw.xserver.loader"
version = "v2.0"
java.sourceCompatibility = JavaVersion.VERSION_21

val outputPath = file("${rootProject.projectDir}/Server")
extra["outputPath"] = outputPath

defaultTasks("build")  // Allow to use `./gradlew` to auto build a full project

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jline:jline:3.26.3")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("commons-cli:commons-cli:1.8.0") // CLI arg
    implementation("com.googlecode.clichemaven:cliche:110413") // CLI cmd
    implementation("org.fusesource.jansi:jansi:2.4.1") // AnsiConsole
    implementation("net.dv8tion:JDA:5.0.2") // JDA
    implementation("commons-io:commons-io:2.16.1") // Commons io
    implementation("org.jsoup:jsoup:1.18.1") // Connection
    implementation("ch.qos.logback:logback-classic:1.5.6") // Log
    implementation("com.google.code.gson:gson:2.11.0") // Json
    implementation("com.charleskorn.kaml:kaml:0.61.0") // Yaml
    implementation("org.apache.commons:commons-text:1.12.0") // StringSubstitutor
    implementation(kotlin("reflect"))
}

tasks.named<ShadowJar>("shadowJar") {
    // [archiveBaseName]-[archiveAppendix]-[archiveVersion]-[archiveClassifier].[archiveExtension]

    archiveBaseName = rootProject.name
    archiveAppendix = "${properties["prefix"]}"
    archiveVersion = "$version"
    archiveClassifier = ""
    destinationDirectory = outputPath

    manifest {
        attributes("Main-Class" to "tw.xserver.loader.MainKt")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

kotlin {
    jvmToolchain(21)
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(project(":"))
        compileOnly("net.dv8tion:JDA:5.0.0") // JDA
        compileOnly("ch.qos.logback:logback-classic:1.5.6") // Log
        compileOnly("com.charleskorn.kaml:kaml:0.60.0") // Yaml
        compileOnly("com.google.code.gson:gson:2.10.1") // Json
        compileOnly("commons-io:commons-io:2.15.1") // Commons io
        compileOnly("org.apache.commons:commons-text:1.12.0") // StringSubstitutor
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget = JvmTarget.JVM_21
    }

    tasks.build {
        dependsOn(tasks.jar)
    }
}
