package tw.xinshou.discord.webdashboard

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import tw.xinshou.discord.webdashboard.api.DashboardBackend
import tw.xinshou.discord.webdashboard.api.UnavailableDashboardBackend
import tw.xinshou.discord.webdashboard.api.configureDashboardRoutes

private val logger = LoggerFactory.getLogger("WebDashboard")

data class DashboardRuntimeConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
) {
    companion object {
        fun fromEnv(): DashboardRuntimeConfig {
            val enabled = System.getenv("XSBOT_DASHBOARD_ENABLED")
                ?.equals("false", ignoreCase = true)
                ?.not()
                ?: true
            val host = System.getenv("XSBOT_DASHBOARD_HOST") ?: "127.0.0.1"
            val port = (System.getenv("XSBOT_DASHBOARD_PORT") ?: "21100").toIntOrNull() ?: 21100

            return DashboardRuntimeConfig(
                enabled = enabled,
                host = host,
                port = port,
            )
        }
    }
}

object DashboardServer {
    private var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var backend: DashboardBackend = UnavailableDashboardBackend

    @Synchronized
    fun start(
        config: DashboardRuntimeConfig = DashboardRuntimeConfig.fromEnv(),
        backend: DashboardBackend = UnavailableDashboardBackend,
    ) {
        if (!config.enabled) {
            logger.info("Web dashboard disabled by XSBOT_DASHBOARD_ENABLED=false.")
            return
        }

        if (engine != null) {
            logger.info("Web dashboard already started.")
            return
        }

        this.backend = backend
        val app = embeddedServer(Netty, host = config.host, port = config.port) {
            configureHttp(host = config.host, port = config.port)
            configureDashboardRoutes(host = config.host, port = config.port, backend = this@DashboardServer.backend)
        }

        app.start(wait = false)
        engine = app
        logger.info("Web dashboard started on http://{}:{}", config.host, config.port)
    }

    @Synchronized
    fun stop(gracePeriodMillis: Long = 700, timeoutMillis: Long = 2_500) {
        val running = engine ?: return
        runCatching { running.stop(gracePeriodMillis, timeoutMillis) }
            .onFailure { logger.error("Web dashboard stop failed.", it) }
        engine = null
        backend = UnavailableDashboardBackend
        logger.info("Web dashboard stopped.")
    }
}

fun main() {
    val config = DashboardRuntimeConfig.fromEnv()
    if (!config.enabled) {
        logger.info("Web dashboard disabled by XSBOT_DASHBOARD_ENABLED=false. Exit.")
        return
    }

    embeddedServer(Netty, host = config.host, port = config.port) {
        configureHttp(host = config.host, port = config.port)
        configureDashboardRoutes(host = config.host, port = config.port, backend = UnavailableDashboardBackend)
    }.start(wait = true)
}

internal fun Application.configureHttp(host: String, port: Int) {
    install(CallLogging)

    install(Compression) {
        gzip {
            priority = 1.0
        }
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)

        allowHost("$host:$port", schemes = listOf("http"))
        allowHost("localhost:5173", schemes = listOf("http"))
        allowHost("127.0.0.1:5173", schemes = listOf("http"))
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Dashboard API error", cause)
            call.respond(
                mapOf(
                    "ok" to false,
                    "message" to "Unexpected server error",
                )
            )
        }
    }
}
