package tw.xinshou.loader.cli

import kotlinx.coroutines.*
import org.jline.reader.*
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.BotLoader
import kotlin.coroutines.coroutineContext

internal class CustomCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val buffer = line.line()
        val tokens = buffer.split(" ")

        if (tokens.size == 1) {
            val commands = listOf("reload", "stop", "exit", "shutdown")
            commands.filter { it.startsWith(tokens[0], ignoreCase = true) }
                .forEach { candidates.add(Candidate(it)) }
        }
//        else if (tokens.size == 2) {
//            when (tokens[0].lowercase()) {
//                "reload" -> {
//                    val reloadOptions = listOf("force", "soft")
//                    reloadOptions.filter { it.startsWith(tokens[1], ignoreCase = true) }
//                        .forEach { candidates.add(Candidate(it)) }
//                }
//            }
//        }
    }
}


internal object JLineManager {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val terminal: Terminal = TerminalBuilder.builder().system(true).build()
    private val completer: Completer = CustomCompleter()
    val reader: LineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(completer)
        .build()

    fun start(scope: CoroutineScope, stopSignal: CompletableDeferred<Unit>) {
        scope.launch {
            mainLoop(stopSignal)
        }
        logger.info("JLineManager started.")
    }

    fun stop() {
        try {
            terminal.close()
        } catch (e: Exception) {
            logger.warn("Error closing terminal: ", e)
        }
        logger.info("JLineManager stopped.")
    }

    private suspend fun mainLoop(stopSignal: CompletableDeferred<Unit>) {
        loop@ while (coroutineContext.isActive) {
            try {
                val cmd = withContext(Dispatchers.IO) {
                    reader.readLine("console > ").trim()
                }
                if (cmd.isNotEmpty()) {
                    when (cmd.lowercase()) {
                        "reload" -> {
                            BotLoader.reload()
                        }

                        "stop", "exit", "shutdown" -> {
                            logger.info("Stopping application as per user command.")
                            stopSignal.complete(Unit)
                            break@loop // 退出循環
                        }

                        else -> {
                            logger.warn("Unknown command: {}", cmd)
                        }
                    }
                }
            } catch (e: UserInterruptException) {
                logger.info("Interrupted by user: ^C")
            } catch (e: EndOfFileException) {
                logger.info("Interrupted by user: ^D")
                stopSignal.complete(Unit)
                break@loop
            } catch (e: Exception) {
                logger.error("An error occurred: ", e)
            }
        }
    }
}
