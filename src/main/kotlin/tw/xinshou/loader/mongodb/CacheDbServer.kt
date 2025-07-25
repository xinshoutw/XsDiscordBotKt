package tw.xinshou.loader.mongodb

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import de.flapdoodle.embed.mongo.commands.ServerAddress
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
import tw.xinshou.loader.base.BotLoader.ROOT_PATH
import java.nio.file.Files
import java.nio.file.Paths

/**
 * CacheDbServer 為單例物件，負責啟動嵌入式 MongoDB 服務，
 * 並提供統一的 MongoClient 存取。使用完畢後請呼叫 close() 釋放資源。
 *
 * 此版本透過指定 customDbPath 並檢查是否存在，不存在時會自動建立目錄，
 * 確保 MongoDB 的資料檔案可以存放在你期望的位置。
 */
object CacheDbServer : AutoCloseable {
    private val DB_PATH = Paths.get(ROOT_PATH, "mongodb-data")
    private val logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var runningMongod: TransitionWalker.ReachedState<RunningMongodProcess>
    private lateinit var mongoClient: MongoClient
    internal val dbScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 啟動嵌入式 MongoDB 服務，並建立 MongoClient 實例。
     * 啟動前會檢查 customDbPath 是否存在，不存在則建立該目錄。
     */
    fun start() {
        if (!Files.exists(DB_PATH)) {
            Files.createDirectories(DB_PATH)
            logger.info("建立資料庫目錄：{}", DB_PATH.toAbsolutePath())
        } else {
            logger.info("資料庫目錄已存在：{}", DB_PATH.toAbsolutePath())
        }


        runningMongod = Mongod.builder()
            .databaseDir(
                Start.to(DatabaseDir::class.java)
                    .initializedWith(DatabaseDir.of(DB_PATH))
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

        val serverAddress: ServerAddress = runningMongod.current().serverAddress
        mongoClient = MongoClients.create("mongodb://${serverAddress.host}:${serverAddress.port}")
        logger.info("Embedded MongoDB started at ${serverAddress.host}:${serverAddress.port}")
    }

    /**
     * 取得指定名稱的資料庫。
     *
     * @param databaseName 資料庫名稱
     * @return MongoDatabase 實例
     */
    fun getDatabase(databaseName: String): MongoDatabase = mongoClient.getDatabase(databaseName)

    /**
     * 取得內部使用的 MongoClient 實例。
     *
     * @return MongoClient 實例
     */
    fun getMongoClient() = mongoClient

    /**
     * 關閉 MongoClient 並停止嵌入式 MongoDB 服務。
     */
    override fun close() {
        mongoClient.close()
        runningMongod.close()
    }
}
