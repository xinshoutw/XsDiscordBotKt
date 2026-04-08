package tw.xinshou.discord.core.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

object Arguments {
    var forceRenewLangResources: Boolean = false
        private set
    var ignoreUpdate: Boolean = false
        private set
    var noOnline: Boolean = false
        private set
    var autoDeferInteractionReplies: Boolean = true
        private set
    var token: String? = null
        private set
    var logLevel: String = "INFO"
        private set

    fun parse(args: Array<String>) {
        val command = object : CliktCommand(name = "xsdiscordbot") {
            val forceRenewLang by option(
                "-Flang", "--force-renew-lang-resources",
                help = "Force re-export all plugin lang resource files, overwriting existing files"
            ).flag(default = false)
            val ignore by option("-I", "--ignore-update", help = "Ignore update checks")
                .flag(default = false)
            val offline by option("-N", "--no-online", help = "Disable online features")
                .flag(default = false)
            val autoDefer by option(
                "--auto-defer-interactions",
                help = "Auto-defer interaction replies"
            ).flag("--no-auto-defer-interactions", default = true)
            val tokenOpt by option("-t", "--token", help = "Bot token")
            val logLevelOpt by option("-l", "--log-level", help = "Log level")
                .default("INFO")

            override fun run() {
                forceRenewLangResources = forceRenewLang
                ignoreUpdate = ignore
                noOnline = offline
                autoDeferInteractionReplies = autoDefer
                token = tokenOpt
                logLevel = logLevelOpt
            }
        }
        command.main(args.toList())
    }
}
