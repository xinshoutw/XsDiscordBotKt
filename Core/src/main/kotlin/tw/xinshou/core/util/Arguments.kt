package tw.xinshou.core.util

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import tw.xinshou.core.logger.LogBackManager


internal object Arguments : CliktCommand() {
    val forceRenewLangResources: Boolean
            by option(
                "-Flang",
                "--force-renew-lang-resources",
                help = "WARNING, Force re-export all plugin lang resource files, overwriting existing files.",
            ).flag(default = false)


    val ignoreVersionCheck: Boolean
            by option(
                "-I",
                "--ignore-update",
                help = "Ignore the version check from GitHub"
            ).flag(default = false)

    val noBuild: Boolean
            by option(
                "-N",
                "--no-online",
                help = "Do not let bot online"
            ).flag(default = false)

    val botToken: String?
            by option(
                "-t",
                "--token",
                help = "Set bot token",
            )

    private val logLevel: String
            by option(
                "-l",
                "--level",
                help = "Set logging level"
            ).default("INFO")

    override fun run() {
        LogBackManager.setLevel(Level.toLevel(logLevel))
    }
}
