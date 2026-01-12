// ███████ ███████ ████████ ████████ ██ ███    ██  ██████
// ██      ██         ██       ██    ██ ████   ██ ██
// ███████ █████      ██       ██    ██ ██ ██  ██ ██   ███
//      ██ ██         ██       ██    ██ ██  ██ ██ ██    ██
// ███████ ███████    ██       ██    ██ ██   ████  ██████


plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") apply false
    id("com.gradleup.shadow") apply false
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

extra["outputPath"] = if (project.hasProperty("outputPath")) {
    println("outputPath set to: ${project.property("outputPath")}")
    file(project.property("outputPath") as String)
} else {
    println("outputPath not set, using default: ${rootProject.projectDir}/DevServer")
    file("${rootProject.projectDir}/DevServer")
}

val versionMajor = findProperty("version.major")?.toString() ?: error("version.major missing")
val versionMinor = findProperty("version.minor")?.toString() ?: error("version.minor missing")
val coreApiVersion = "$versionMajor.$versionMinor"
val updateReadmeVersion by tasks.registering {
    description = "Updates the version badge in README.md with current version from gradle.properties"
    group = "documentation"

    val readmeFile = file("README.md")
    inputs.property("coreApiVersion", coreApiVersion)
    outputs.file(readmeFile)

    doLast {
        if (readmeFile.exists()) {
            val content = readmeFile.readText()
            val updatedContent = content.replace(
                Regex("""!\[Version\]\(https://img\.shields\.io/badge/version-[\d\.\*]+-blue\?style=for-the-badge&logo=github\)"""),
                "![Version](https://img.shields.io/badge/version-$coreApiVersion.*-blue?style=for-the-badge&logo=github)"
            )
            readmeFile.writeText(updatedContent)
            println("Updated README.md version badge to: $coreApiVersion.*")
        }
    }
}

// Hook updateReadmeVersion into common development tasks
// This ensures README version is updated during normal development workflow
tasks.named("build") {
    dependsOn(updateReadmeVersion)
}
