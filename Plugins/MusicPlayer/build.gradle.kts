group = "tw.xinshou.plugin"
version = "0.1.0"

plugins {
    id("xs-plugin-shadow")
}

repositories {
    maven(url = "https://maven.lavalink.dev/releases") // common
    maven(url = "https://maven.topi.wtf/releases") // lavasrc
    maven(url = "https://jitpack.io") // lavaplayer
}

dependencies {
    compileOnly(project(":Plugins:API:AudioAPI"))
    implementation("dev.lavalink.youtube:common:1.13.3")
    implementation("com.github.topi314.lavasrc:lavasrc:4.7.3")
    implementation("com.github.topi314.lavasrc:lavasrc-protocol:4.7.3")
    implementation("dev.arbjerg:lavaplayer:2.2.4")
}
