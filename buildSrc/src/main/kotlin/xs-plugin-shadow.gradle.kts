plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

plugins.withId("com.gradleup.shadow") {
    tasks.named("shadowJar") {
        val outputPath: File by rootProject.extra
        setProperty("archiveFileName", "${project.name}-${properties["prefix"]}-$version.jar")
        setProperty("destinationDirectory", outputPath.resolve("plugins"))
    }
}