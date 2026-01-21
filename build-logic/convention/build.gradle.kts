plugins {
    `kotlin-dsl`
}

group = "tw.xinshou.buildlogic"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization)
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.idea.ext.gradle.plugin)
}

kotlin {
    jvmToolchain(21)
}
