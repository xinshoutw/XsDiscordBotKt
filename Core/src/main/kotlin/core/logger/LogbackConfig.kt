package core.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.fusesource.jansi.AnsiConsole
import org.slf4j.LoggerFactory

object LogbackConfig {
    /** Installs ANSI console support for colored output. */
    fun configureSystem() {
        AnsiConsole.systemInstall()
    }

    /** Uninstalls ANSI console support. */
    fun uninstall() {
        AnsiConsole.systemUninstall()
    }

    /** Sets the ROOT logger level to the given [level] string (e.g. "INFO", "DEBUG"). */
    fun setLevel(level: String) {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.getLogger("ROOT").level = Level.valueOf(level)
    }
}
