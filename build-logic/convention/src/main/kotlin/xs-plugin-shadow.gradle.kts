plugins {
    id("xs-jvm-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":Core"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

plugins.withId("com.gradleup.shadow") {
    tasks.named("shadowJar") {
        val outputPath: File by rootProject.extra
        setProperty("archiveFileName", "${project.name}-$version.jar")
        setProperty("destinationDirectory", outputPath.resolve("plugins"))
    }
}