import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    id("xs-jvm-conventions")
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("signing")
}

repositories { mavenCentral() }


java {
    withSourcesJar()
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(group.toString(), "discord-bot-core", version.toString())

    pom {
        name.set("discord-bot-core")
        description.set("This is a discord bot core. You can add plugins in your own!")
        inceptionYear.set("2026")
        url.set("https://github.com/xinshoutw/XsDiscordBotKt")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("xinshoutw")
                name.set("xinshoutw")
                url.set("https://github.com/xinshoutw/")
            }
        }

        scm {
            url.set("https://github.com/xinshoutw/XsDiscordBotKt")
            connection.set("scm:git:git://github.com/xinshoutw/XsDiscordBotKt.git")
            developerConnection.set("scm:git:ssh://git@github.com/xinshoutw/XsDiscordBotKt.git")
        }
    }
}

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

    implementation(libs.embed.mongo)
    implementation(libs.protobuf.java)
    implementation(libs.jline)
    implementation(libs.clikt)
    implementation(libs.jansi)
    implementation(libs.jsoup)
    implementation(libs.kotlin.reflect)
}

try {
    tasks.named("sourcesJar") {
        dependsOn("generateVersion")
    }
} catch (e: Exception) { /* ignore */
}

tasks.named<ShadowJar>("shadowJar") {
    val outputPath: File by rootProject.extra
    archiveFileName.set("${project.name}-$version.jar")
    destinationDirectory.set(outputPath)
    manifest {
        attributes("Main-Class" to "tw.xinshou.discord.core.MainKt")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}