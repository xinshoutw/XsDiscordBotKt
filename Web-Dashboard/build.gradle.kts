import org.gradle.api.GradleException
import java.io.File

plugins {
    id("xs-jvm-conventions")
    application
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("tw.xinshou.discord.webdashboard.MainKt")
}

val frontendDir = layout.projectDirectory.dir("frontend")
val frontendDistDir = frontendDir.dir("dist")
val nodeModulesDir = frontendDir.dir("node_modules")
val npmCacheDir = layout.buildDirectory.dir("npm-cache")
val isWindows = System.getProperty("os.name").startsWith("Windows")
val userHome = System.getProperty("user.home")
val npmDefaultFromEnv = System.getenv("NPM_EXECUTABLE")
val nvmBin = System.getenv("NVM_BIN")
val npmDefaultFromNvm = if (!nvmBin.isNullOrBlank()) "${nvmBin.trimEnd('/', '\\')}/npm" else null
val npmDefaultFromNvmHome = if (!isWindows) {
    val nvmVersionsDir = file("$userHome/.nvm/versions/node")
    if (nvmVersionsDir.exists() && nvmVersionsDir.isDirectory) {
        nvmVersionsDir
            .listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }
            ?.map { File(it, "bin/npm") }
            ?.firstOrNull { it.exists() && it.isFile }
            ?.absolutePath
    } else {
        null
    }
} else {
    null
}
val npmDefaultFromVolta = if (!isWindows && file("$userHome/.volta/bin/npm").exists()) "$userHome/.volta/bin/npm" else null
val npmDefaultFromMacHomebrew = if (!isWindows && file("/opt/homebrew/bin/npm").exists()) "/opt/homebrew/bin/npm" else null
val npmDefaultFromUsrLocal = if (!isWindows && file("/usr/local/bin/npm").exists()) "/usr/local/bin/npm" else null
val npmCommand = providers.gradleProperty("frontend.npmExecutable")
    .orElse(
        npmDefaultFromEnv
            ?: npmDefaultFromNvm
            ?: npmDefaultFromNvmHome
            ?: npmDefaultFromVolta
            ?: npmDefaultFromMacHomebrew
            ?: npmDefaultFromUsrLocal
            ?: if (isWindows) "npm.cmd" else "npm"
    )

val frontendPath = listOfNotNull(
    System.getenv("PATH"),
    nvmBin,
    if (!isWindows) "/opt/homebrew/bin" else null,
    if (!isWindows) "/usr/local/bin" else null
)
    .flatMap { it.split(File.pathSeparatorChar) }
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .distinct()
    .joinToString(File.pathSeparator)
val skipFrontendNpm = providers.gradleProperty("frontend.skipNpm")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

val frontendInstall by tasks.registering(Exec::class) {
    group = "frontend"
    description = "Installs frontend dependencies"
    workingDir = frontendDir.asFile
    commandLine(npmCommand.get(), "ci")
    environment("NPM_CONFIG_CACHE", npmCacheDir.get().asFile.absolutePath)
    environment("PATH", frontendPath)
    onlyIf { !skipFrontendNpm.get() }

    doFirst {
        if (npmCommand.get().isBlank()) {
            throw GradleException(
                "npm executable is empty. Set -Pfrontend.npmExecutable=/absolute/path/to/npm"
            )
        }
    }

    inputs.files(
        frontendDir.file("package.json"),
        frontendDir.file("package-lock.json")
    )
    outputs.dir(nodeModulesDir)
}

val frontendBuild by tasks.registering(Exec::class) {
    group = "frontend"
    description = "Builds the React dashboard"
    workingDir = frontendDir.asFile
    commandLine(npmCommand.get(), "run", "build")
    environment("NPM_CONFIG_CACHE", npmCacheDir.get().asFile.absolutePath)
    environment("PATH", frontendPath)
    dependsOn(frontendInstall)
    onlyIf { !skipFrontendNpm.get() }

    inputs.files(
        frontendDir.file("package.json"),
        frontendDir.file("package-lock.json"),
        frontendDir.file("index.html"),
        frontendDir.file("vite.config.ts"),
        frontendDir.file("tsconfig.json"),
        frontendDir.file("tsconfig.node.json"),
        frontendDir.file("tailwind.config.ts"),
        frontendDir.file("postcss.config.js"),
        frontendDir.file("components.json")
    )
    inputs.dir(frontendDir.dir("src"))
    outputs.dir(frontendDistDir)
}

val frontendDistCheck by tasks.registering {
    group = "frontend"
    description = "Checks dashboard bundle exists and is a production build."

    doLast {
        if (!frontendDistDir.asFile.exists()) {
            throw org.gradle.api.GradleException(
                "Frontend bundle not found at ${frontendDistDir.asFile}. " +
                    "Run `cd ${frontendDir.asFile} && npm install && npm run build` first, " +
                    "or run Gradle without -Pfrontend.skipNpm=true."
            )
        }

        val builtIndex = frontendDistDir.file("index.html").asFile
        if (!builtIndex.exists()) {
            throw org.gradle.api.GradleException(
                "Frontend bundle missing index.html at ${builtIndex.absolutePath}. " +
                    "Run `cd ${frontendDir.asFile} && npm run build` first."
            )
        }

        val html = builtIndex.readText()
        if (html.contains("""<script type="module" src="/src/main.tsx"></script>""")) {
            throw org.gradle.api.GradleException(
                "Detected Vite dev entry in dist/index.html (/src/main.tsx). " +
                    "This is not a production bundle. Run `npm run build` in ${frontendDir.asFile}."
            )
        }
    }
}
frontendDistCheck {
    mustRunAfter(frontendBuild)
}

tasks.processResources {
    dependsOn(frontendBuild)
    dependsOn(frontendDistCheck)
    from(frontendDistDir) {
        into("dashboard")
    }
}

tasks.named("build") {
    dependsOn(frontendBuild)
    dependsOn(frontendDistCheck)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.compression)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
}
