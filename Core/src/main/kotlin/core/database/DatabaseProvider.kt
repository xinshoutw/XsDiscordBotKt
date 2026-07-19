package tw.xinshou.discord.core.database

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.flapdoodle.embed.mongo.commands.ServerAddress
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.embed.mongo.types.DatabaseDir
import de.flapdoodle.embed.mongo.types.DistributionBaseUrl
import de.flapdoodle.embed.process.io.ProcessOutput
import de.flapdoodle.embed.process.io.Processors
import de.flapdoodle.embed.process.io.Slf4jLevel
import de.flapdoodle.embed.process.io.directories.PersistentDir
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.transitions.Start
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory
import java.net.BindException
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DatabaseProvider(private val config: tw.xinshou.discord.core.config.BotConfig.DatabaseConfig) {
    lateinit var client: MongoClient
        private set

    private var embeddedProcess: TransitionWalker.ReachedState<RunningMongodProcess>? = null

    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val ENV_MONGODB_PORT = "XS_MONGODB_PORT"
        private const val ENV_MONGODB_DB_PATH = "XS_MONGODB_DB_PATH"
        private const val ENV_MONGODB_INSTANCE = "XS_MONGODB_INSTANCE"
        private const val AUTO_PORT_RETRY = 8

        val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * Start the database connection.
     * If [config.embedded] is true, an embedded MongoDB instance is started first.
     * Otherwise, connects using [config.connectionString].
     */
    suspend fun start() {
        if (config.embedded) {
            startEmbedded()
        } else {
            client = MongoClient.create(config.connectionString)
            logger.info("Connected to external MongoDB at {}", config.connectionString)
        }
    }

    fun database(name: String): MongoDatabase = client.getDatabase(name)

    suspend fun stop() {
        if (::client.isInitialized) {
            client.close()
        }
        embeddedProcess?.close()
        embeddedProcess = null
        logger.info("Database connection closed.")
    }

    private fun startEmbedded() {
        val dbPath = resolveDbPath()
        if (!Files.exists(dbPath)) {
            Files.createDirectories(dbPath)
            logger.info("Created database directory: {}", dbPath.toAbsolutePath())
        }

        val configuredPort = readConfiguredPort()
        if (configuredPort != null) {
            startWithPort(dbPath, configuredPort, isConfiguredPort = true)
            return
        }

        var lastError: Exception? = null
        repeat(AUTO_PORT_RETRY) { attempt ->
            val port = randomFreePort()
            try {
                startWithPort(dbPath, port, isConfiguredPort = false)
                return
            } catch (e: Exception) {
                lastError = e
                if (!e.isBindException()) throw e
                logger.warn("MongoDB port {} is in use, retry {}/{}", port, attempt + 1, AUTO_PORT_RETRY)
            }
        }

        throw IllegalStateException("Failed to start embedded MongoDB after $AUTO_PORT_RETRY retries.", lastError)
    }

    private fun startWithPort(dbPath: Path, port: Int, isConfiguredPort: Boolean) {
        var startedProcess: TransitionWalker.ReachedState<RunningMongodProcess>? = null
        var mongoClient: MongoClient? = null
        try {
            startedProcess = Mongod.builder()
                .databaseDir(
                    Start.to(DatabaseDir::class.java)
                        .initializedWith(DatabaseDir.of(dbPath))
                )
                .net(
                    Start.to(Net::class.java)
                        .initializedWith(Net.defaults().withPort(port))
                )
                .processOutput(
                    Start.to(ProcessOutput::class.java).initializedWith(
                        ProcessOutput.builder()
                            .output(Processors.named("", Processors.logTo(logger, Slf4jLevel.INFO)))
                            .error(Processors.named("", Processors.logTo(logger, Slf4jLevel.ERROR)))
                            .commands(Processors.logTo(logger, Slf4jLevel.DEBUG))
                            .build()
                    )
                )
                .distributionBaseUrl(
                    Start.to(DistributionBaseUrl::class.java)
                        .initializedWith(DistributionBaseUrl.of("https://fastdl.mongodb.org"))
                )
                .persistentBaseDir(
                    Start.to(PersistentDir::class.java)
                        .providedBy(
                            PersistentDir.inWorkingDir(".embedmongo")
                                .mapToUncheckedException { RuntimeException(it) }
                        )
                )
                .build()
                .start(Version.Main.V8_2)

            val serverAddress: ServerAddress = startedProcess.current().serverAddress
            mongoClient = MongoClient.create("mongodb://${serverAddress.host}:${serverAddress.port}")

            embeddedProcess = startedProcess
            client = mongoClient

            logger.info(
                "Embedded MongoDB started at {}:{} (dbPath={})",
                serverAddress.host,
                serverAddress.port,
                dbPath.toAbsolutePath(),
            )
        } catch (e: Exception) {
            mongoClient?.close()
            startedProcess?.close()
            if (isConfiguredPort && e.isBindException()) {
                throw IllegalStateException(
                    "Configured port $port ($ENV_MONGODB_PORT) is already in use.",
                    e
                )
            }
            throw e
        }
    }

    private fun resolveDbPath(): Path {
        val envPath = System.getenv(ENV_MONGODB_DB_PATH)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (envPath != null) return Paths.get(envPath)

        val instanceSuffix = System.getenv(ENV_MONGODB_INSTANCE)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val dirName = if (instanceSuffix != null) {
            "${config.dataPath}-$instanceSuffix"
        } else {
            config.dataPath
        }
        return Paths.get(dirName)
    }

    private fun readConfiguredPort(): Int? {
        val raw = System.getenv(ENV_MONGODB_PORT)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val parsed = raw.toIntOrNull()
            ?: throw IllegalArgumentException("$ENV_MONGODB_PORT is not a valid number: $raw")

        require(parsed in 1..65535) {
            "$ENV_MONGODB_PORT must be in 1..65535, got $parsed"
        }
        return parsed
    }

    private fun randomFreePort(): Int {
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            return socket.localPort
        }
    }

    private fun Throwable.isBindException(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is BindException) return true
            current = current.cause
        }
        val message = this.message?.lowercase() ?: return false
        return "address already in use" in message || "bind" in message
    }
}
