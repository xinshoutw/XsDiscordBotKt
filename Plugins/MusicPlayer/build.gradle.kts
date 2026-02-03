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
    implementation("dev.lavalink.youtube:common:1.17.0")
    implementation("com.github.topi314.lavasrc:lavasrc:4.8.1")
    implementation("com.github.topi314.lavasrc:lavasrc-protocol:4.8.1")
    implementation("dev.arbjerg:lavaplayer:2.2.6")
}
