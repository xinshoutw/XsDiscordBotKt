plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":"))
}


tasks.build {
    dependsOn(tasks.jar)
}

tasks.named<Jar>("jar") {
    val outputPath: File by rootProject.extra
    setProperty("archiveFileName", "${project.name}-${properties["prefix"]}-$version.jar")
    setProperty("destinationDirectory", outputPath.resolve("plugins"))
}
