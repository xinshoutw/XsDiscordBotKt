package tw.xserver.loader.util

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import tw.xserver.loader.logger.LogBackManager


internal object Arguments : CliktCommand() {
    val forceReplaceLangResources: Boolean
            by option(
                "-F",
                "--force-export-lang-resources",
                help = "Force all plugins export theirs language resources"
            ).flag(default = false)

    val forceRenewLangResources: Boolean
            by option(
                "-FF",
                "--force-renew-lang-resources",
                help = "Force all plugins delete lang directory, then export theirs language resources"
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
