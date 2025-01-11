package tw.xserver.loader.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.fusesource.jansi.AnsiConsole
import org.slf4j.LoggerFactory

internal object LogBackManager {
    private val rootLogger =
        (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(Logger.ROOT_LOGGER_NAME)

    fun configureSystem() {
        System.setProperty("jansi.passthrough", "true")
        AnsiConsole.systemInstall()
    }

    fun uninstall() {
        AnsiConsole.systemUninstall()
    }

    fun setLevel(logLevel: Level) {
        rootLogger.level = logLevel
    }
}