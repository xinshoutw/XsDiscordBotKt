group = "tw.xinshou.api"
version = "0.2.0"

plugins {
    id("xs-plugin-shadow")
}

dependencies {
    api("org.xerial:sqlite-jdbc:3.50.3.0")
    api("org.jetbrains.exposed:exposed-core:0.61.0")
    api("org.jetbrains.exposed:exposed-dao:0.61.0")
    api("org.jetbrains.exposed:exposed-jdbc:0.61.0")
}