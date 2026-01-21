// ███████ ███████ ████████ ████████ ██ ███    ██  ██████
// ██      ██         ██       ██    ██ ████   ██ ██
// ███████ █████      ██       ██    ██ ██ ██  ██ ██   ███
//      ██ ██         ██       ██    ██ ██  ██ ██ ██    ██
// ███████ ███████    ██       ██    ██ ██   ████  ██████


plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.idea.ext) apply false
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
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
