import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


// ███████ ███████ ████████ ████████ ██ ███    ██  ██████
// ██      ██         ██       ██    ██ ████   ██ ██
// ███████ █████      ██       ██    ██ ██ ██  ██ ██   ███
//      ██ ██         ██       ██    ██ ██  ██ ██ ██    ██
// ███████ ███████    ██       ██    ██ ██   ████  ██████

group = "tw.xinshou.loader"
version = "v2.0.1"

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

// ██████  ███████ ██████  ███████ ███    ██ ██████  ███████ ███    ██  ██████ ██    ██
// ██   ██ ██      ██   ██ ██      ████   ██ ██   ██ ██      ████   ██ ██       ██  ██
// ██   ██ █████   ██████  █████   ██ ██  ██ ██   ██ █████   ██ ██  ██ ██        ████
// ██   ██ ██      ██      ██      ██  ██ ██ ██   ██ ██      ██  ██ ██ ██         ██
// ██████  ███████ ██      ███████ ██   ████ ██████  ███████ ██   ████  ██████    ██

repositories { mavenCentral() }

dependencies {
    compileOnly(libs.annotations)
    api(libs.jda)
    api(libs.logback.classic)
    api(libs.kaml)
    api(libs.commons.io)
    api(libs.commons.text)
    api(libs.mongodb.driver.kotlin.sync)
    api(libs.moshi.kotlin)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.embed.mongo) // Embedded MongoDb
    implementation(libs.protobuf.java) // CVE fix
    implementation(libs.jline) // CLI
    implementation(libs.clikt) // Run Arg
    implementation(libs.jansi) // AnsiConsole
    implementation(libs.jsoup) // Connection
    implementation(libs.kotlin.reflect)
}


// ██████  ██    ██ ██ ██      ██████
// ██   ██ ██    ██ ██ ██      ██   ██
// ██████  ██    ██ ██ ██      ██   ██
// ██   ██ ██    ██ ██ ██      ██   ██
// ██████   ██████  ██ ███████ ██████

defaultTasks("build")  // Allow to use `./gradlew` to auto build a full project

val outputPath = if (project.hasProperty("outputPath")) {
    file(project.property("outputPath") as String)
} else {
    file("${rootProject.projectDir}/DevServer")
}


extra["outputPath"] = outputPath
tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-${properties["prefix"]}-$version.jar")
    destinationDirectory.set(outputPath)
    manifest {
        attributes("Main-Class" to "tw.xinshou.loader.MainKt")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}