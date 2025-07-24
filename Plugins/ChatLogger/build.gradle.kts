group = "tw.xinshou.plugin"
version = "0.2.0"

plugins {
    id("xs-plugin")
}

dependencies {
    compileOnly(project(":Plugins:API:SQLiteAPI"))
}
