package tw.xinshou.discord.core.mongodb

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
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
import tw.xinshou.discord.core.base.BotLoader.ROOT_PATH
import java.net.BindException
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * CacheDbServer 為單例物件，負責啟動嵌入式 MongoDB 服務，
 * 並提供統一的 MongoClient 存取。使用完畢後請呼叫 close() 釋放資源。
 *
 * 此版本透過指定 customDbPath 並檢查是否存在，不存在時會自動建立目錄，
 * 確保 MongoDB 的資料檔案可以存放在你期望的位置。
 */
object CacheDbServer : AutoCloseable {
    private const val DEFAULT_DB_DIR_NAME = "mongodb-data"
    private const val ENV_MONGODB_PORT = "XS_MONGODB_PORT"
    private const val ENV_MONGODB_INSTANCE = "XS_MONGODB_INSTANCE"
    private const val ENV_MONGODB_DB_PATH = "XS_MONGODB_DB_PATH"
    private const val AUTO_PORT_RETRY = 8

    private val logger = LoggerFactory.getLogger(this::class.java)
    private var runningMongod: TransitionWalker.ReachedState<RunningMongodProcess>? = null
    private var mongoClient: MongoClient? = null
    internal val dbScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 啟動嵌入式 MongoDB 服務，並建立 MongoClient 實例。
     * 啟動前會檢查 customDbPath 是否存在，不存在則建立該目錄。
     */
    fun start() {
        if (runningMongod != null) {
            logger.warn("Embedded MongoDB already started, skipping duplicated start() call.")
            return
        }

        val dbPath = resolveDbPath()
        if (!Files.exists(dbPath)) {
            Files.createDirectories(dbPath)
            logger.info("建立資料庫目錄：{}", dbPath.toAbsolutePath())
        } else {
            logger.info("資料庫目錄已存在：{}", dbPath.toAbsolutePath())
        }

        val configuredPort = readConfiguredPort()
        if (configuredPort != null) {
            startWithPort(dbPath = dbPath, port = configuredPort, isConfiguredPort = true)
            return
        }

        var lastError: Exception? = null
        repeat(AUTO_PORT_RETRY) { attempt ->
            val port = randomFreePort()
            try {
                startWithPort(dbPath = dbPath, port = port, isConfiguredPort = false)
                return
            } catch (e: Exception) {
                lastError = e
                if (!e.isBindException()) throw e
                logger.warn(
                    "MongoDB 埠 {} 已被占用，重試 {}/{}",
                    port,
                    attempt + 1,
                    AUTO_PORT_RETRY
                )
            }
        }

        throw IllegalStateException("無法在 $AUTO_PORT_RETRY 次重試內啟動 Embedded MongoDB。", lastError)
    }

    private fun startWithPort(dbPath: Path, port: Int, isConfiguredPort: Boolean) {
        var startedProcess: TransitionWalker.ReachedState<RunningMongodProcess>? = null
        var client: MongoClient? = null
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
                .start(Version.Main.V4_4) // ancient version, because Raspberry Pi 4B doesn't support newer versions

            val serverAddress: ServerAddress = startedProcess.current().serverAddress
            client = MongoClients.create("mongodb://${serverAddress.host}:${serverAddress.port}")

            runningMongod = startedProcess
            mongoClient = client

            logger.info(
                "Embedded MongoDB started at {}:{} (dbPath={})",
                serverAddress.host,
                serverAddress.port,
                dbPath.toAbsolutePath(),
            )
        } catch (e: Exception) {
            client?.close()
            startedProcess?.close()
            if (isConfiguredPort && e.isBindException()) {
                throw IllegalStateException(
                    "環境變數 $ENV_MONGODB_PORT=$port 已被占用，請改用其他 port。",
                    e
                )
            }
            throw e
        }
    }

    private fun resolveDbPath(): Path {
        val customDbPath = System.getenv(ENV_MONGODB_DB_PATH)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (customDbPath != null) return Paths.get(customDbPath)

        val instanceSuffix = System.getenv(ENV_MONGODB_INSTANCE)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val dirName = if (instanceSuffix == null) {
            DEFAULT_DB_DIR_NAME
        } else {
            "$DEFAULT_DB_DIR_NAME-$instanceSuffix"
        }
        return Paths.get(ROOT_PATH, dirName)
    }

    private fun readConfiguredPort(): Int? {
        val rawPort = System.getenv(ENV_MONGODB_PORT)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val parsedPort = rawPort.toIntOrNull()
            ?: throw IllegalArgumentException("環境變數 $ENV_MONGODB_PORT 不是有效數字：$rawPort")

        require(parsedPort in 1..65535) {
            "環境變數 $ENV_MONGODB_PORT 必須介於 1..65535，目前為 $parsedPort"
        }
        return parsedPort
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

    /**
     * 取得指定名稱的資料庫。
     *
     * @param databaseName 資料庫名稱
     * @return MongoDatabase 實例
     */
    fun getDatabase(databaseName: String): MongoDatabase = getMongoClient().getDatabase(databaseName)

    /**
     * 取得內部使用的 MongoClient 實例。
     *
     * @return MongoClient 實例
     */
    fun getMongoClient(): MongoClient {
        return mongoClient ?: throw IllegalStateException("CacheDbServer 尚未啟動，請先呼叫 start()")
    }

    /**
     * 關閉 MongoClient 並停止嵌入式 MongoDB 服務。
     */
    override fun close() {
        mongoClient?.close()
        mongoClient = null

        runningMongod?.close()
        runningMongod = null
    }
}
