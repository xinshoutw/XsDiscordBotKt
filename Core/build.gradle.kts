import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    id("xs-jvm-conventions")
}

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

tasks.named<ShadowJar>("shadowJar") {
    val outputPath: File by rootProject.extra
    archiveFileName.set("${project.name}-$version.jar")
    destinationDirectory.set(outputPath)
    manifest {
        attributes("Main-Class" to "tw.xinshou.core.MainKt")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}