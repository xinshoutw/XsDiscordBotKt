import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "tw.xinshou.loader"
version = "v2.0"
java.sourceCompatibility = JavaVersion.VERSION_21

val outputPath = file("${rootProject.projectDir}/Server")
extra["outputPath"] = outputPath

defaultTasks("build")  // Allow to use `./gradlew` to auto build a full project

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")

    api("net.dv8tion:JDA:5.3.0") // JDA
    api("ch.qos.logback:logback-classic:1.5.16") // Log
    api("com.charleskorn.kaml:kaml:0.70.0") // Yaml
    api("com.google.code.gson:gson:2.12.1") // Json
    api("commons-io:commons-io:2.18.0") // Commons io
    api("org.apache.commons:commons-text:1.13.0") // StringSubstitutor
    api("org.mongodb:mongodb-driver-kotlin-sync:5.3.1") // MongoDb
    api("com.squareup.moshi:moshi-kotlin:1.15.2")

    implementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo:4.18.1") // Embedded MongoDb
    implementation("com.google.protobuf:protobuf-java:4.29.3") // CVE fix
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1") // coroutine
    implementation("org.jline:jline:3.29.0") // CLI
    implementation("com.github.ajalt.clikt:clikt:5.0.2") // Run Arg
    implementation("org.fusesource.jansi:jansi:2.4.1") // AnsiConsole
    implementation("org.jsoup:jsoup:1.18.3") // Connection
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
        attributes("Main-Class" to "tw.xinshou.loader.MainKt")
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
