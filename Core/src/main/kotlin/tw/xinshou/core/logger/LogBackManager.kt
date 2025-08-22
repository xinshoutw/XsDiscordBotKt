package tw.xinshou.core.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.fusesource.jansi.AnsiConsole
import org.slf4j.LoggerFactory
import java.io.IOError
import java.io.IOException

internal object LogBackManager {
    private val rootLogger =
        (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(Logger.ROOT_LOGGER_NAME)

    fun configureSystem() {
        System.setProperty("jansi.passthrough", "true")
        AnsiConsole.systemInstall()
    }

    fun uninstall() {
        try {
            AnsiConsole.systemUninstall()
        } catch (e: IOError) {
            // Suppress IOError that occurs when console streams are already closed
            // This typically happens during shutdown when JLineManager closes the terminal first
        } catch (e: IOException) {
            // Suppress IOException that occurs when console streams are already closed
        }
    }

    fun setLevel(logLevel: Level) {
        rootLogger.level = logLevel
    }
}