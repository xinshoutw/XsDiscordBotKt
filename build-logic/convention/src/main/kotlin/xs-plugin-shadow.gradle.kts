import org.jetbrains.gradle.ext.packagePrefix
import org.jetbrains.gradle.ext.settings

plugins {
    id("xs-jvm-conventions")
    id("org.jetbrains.gradle.plugin.idea-ext")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

idea {
    module {
        settings {
            packagePrefix["src/main/kotlin"] = "tw.xinshou.discord.plugin"
            packagePrefix["src/test/kotlin"] = "tw.xinshou.discord.plugin"
        }
    }
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