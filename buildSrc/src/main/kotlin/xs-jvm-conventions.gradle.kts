import org.gradle.internal.jvm.Jvm
import java.time.Instant

plugins {
    `java-library`
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val author = rootProject.findProperty("author")?.toString() ?: error("author missing")
val major = rootProject.findProperty("version.major")?.toString() ?: error("version.major missing").toInt()
val minor = rootProject.findProperty("version.minor")?.toString() ?: error("version.minor missing").toInt()
val patch = findProperty("version.patch")?.toString() ?: error("version.patch missing").toInt()
val snapshot = rootProject.properties["version.snapshot"]?.toString()?.toBooleanStrictOrNull() ?: false

val coreApi = "$major.$minor"
val verString = buildString {
    append("$coreApi.$patch")
    if (snapshot) append("-SNAPSHOT")
}

group = rootProject.findProperty("group") as String
version = verString

val genDir = layout.buildDirectory.dir("generated/sources/version/kotlin")
val generateVersion by tasks.registering {
    outputs.dir(genDir)
    doLast {
        val file = genDir.get().file("tw/xinshou/common/Version.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package tw.xinshou.common
            object Version {
                const val VERSION = "$verString"
                const val CORE_API = "$coreApi"
                val BUILD_TIME: String = "${Instant.now()}"
            }
            """.trimIndent()
        )
    }
}
sourceSets.main { java.srcDir(genDir) }
tasks.named("compileKotlin") { dependsOn(generateVersion) }


tasks.withType(ProcessResources::class.java).configureEach {
    inputs.property("xs.version", verString)
    inputs.property("xs.coreApi", coreApi)

    filteringCharset = "UTF-8"

    filesMatching(listOf("**/info.yaml", "**/info.yml")) {
        expand(
            mapOf(
                "author" to author,
                "name" to project.name,
                "coreApi" to coreApi,
                "version" to verString,
            )
        )
    }
}

tasks.withType(Jar::class.java).configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to verString,
            "X-Core-Api" to coreApi,
            "Built-By" to System.getProperty("user.name"),
            "Build-Jdk" to Jvm.current().javaVersion.toString()
        )
    }
}
