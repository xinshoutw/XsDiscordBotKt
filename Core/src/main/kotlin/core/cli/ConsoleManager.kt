package tw.xinshou.discord.core.cli

import tw.xinshou.discord.core.logger.JLineLogbackAppender
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.TerminalBuilder
import org.slf4j.LoggerFactory

class ConsoleManager(
    private val onReload: suspend () -> Unit,
    private val onStop: suspend () -> Unit,
) {
    private val logger = LoggerFactory.getLogger(ConsoleManager::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope, stopSignal: CompletableDeferred<Unit>) {
        val terminal = TerminalBuilder.builder().system(true).build()
        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(StringsCompleter("reload", "stop", "exit", "shutdown"))
            .build()

        JLineLogbackAppender.lineReader = reader

        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val line = try { reader.readLine("> ") } catch (_: Exception) { break }
                when (line.trim().lowercase()) {
                    "reload" -> {
                        logger.info("Reloading...")
                        onReload()
                        logger.info("Reload complete.")
                    }
                    "stop", "exit", "shutdown" -> {
                        logger.info("Shutting down...")
                        onStop()
                        stopSignal.complete(Unit)
                        break
                    }
                    else -> if (line.isNotBlank()) logger.warn("Unknown command: {}", line)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        JLineLogbackAppender.lineReader = null
    }
}
