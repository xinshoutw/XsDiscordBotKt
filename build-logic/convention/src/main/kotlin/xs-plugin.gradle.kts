plugins {
    id("xs-jvm-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":BotCore"))
}

tasks.build {
    dependsOn(tasks.jar)
}

tasks.named<Jar>("jar") {
    val outputPath: File by rootProject.extra
    setProperty("archiveFileName", "${project.name}-$version.jar")
    setProperty("destinationDirectory", outputPath.resolve("plugins"))
}
