package tw.xinshou.discord.webdashboard.api

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import tw.xinshou.discord.webdashboard.model.CoreConfigDto
import tw.xinshou.discord.webdashboard.model.HealthDto
import tw.xinshou.discord.webdashboard.model.PluginToggleRequestDto
import tw.xinshou.discord.webdashboard.model.PluginYamlUpdateRequestDto
import tw.xinshou.discord.webdashboard.model.SaveResponseDto
import java.util.concurrent.ConcurrentHashMap

private const val DEV_ENTRY = """<script type="module" src="/src/main.tsx"></script>"""

private data class CachedAsset(val bytes: ByteArray, val contentType: ContentType)

private val assetCache = ConcurrentHashMap<String, CachedAsset>()

internal fun Application.configureDashboardRoutes(host: String, port: Int, backend: DashboardBackend) {
    routing {
        get("/src/{...}") {
            call.respondText(
                status = HttpStatusCode.Gone,
                contentType = ContentType.Text.Plain,
                text = "Development path '/src/*' is not available in production bundle."
            )
        }

        get("/assets/{...}") {
            val requestPath = call.request.path()
            val relativePath = requestPath.removePrefix("/assets/")
            if (relativePath.isBlank() || relativePath == requestPath) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val cached = assetCache.getOrPut(relativePath) {
                val resourcePath = "dashboard/assets/$relativePath"
                val resourceBytes = this::class.java.classLoader
                    .getResourceAsStream(resourcePath)
                    ?.readBytes()
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                val contentType = when (relativePath.substringAfterLast('.', "").lowercase()) {
                    "js", "mjs" -> ContentType.Application.JavaScript
                    "css" -> ContentType.Text.CSS
                    "svg" -> ContentType.Image.SVG
                    "png" -> ContentType.Image.PNG
                    "jpg", "jpeg" -> ContentType.Image.JPEG
                    "webp" -> ContentType("image", "webp")
                    "gif" -> ContentType.Image.GIF
                    "json" -> ContentType.Application.Json
                    "map" -> ContentType.Application.Json
                    "woff" -> ContentType("font", "woff")
                    "woff2" -> ContentType("font", "woff2")
                    else -> ContentType.Application.OctetStream
                }

                CachedAsset(resourceBytes, contentType)
            }

            call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            call.respondBytes(cached.bytes, cached.contentType)
        }

        route("/api/v1") {
            get("/health") {
                call.respond(
                    HealthDto(
                        status = "ok",
                        serverTime = System.currentTimeMillis(),
                        mode = backend.mode(),
                        recommendedBaseUrl = "http://$host:$port",
                    )
                )
            }

            route("/config") {
                get("/core") {
                    runCatching { backend.getCoreConfig() }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respondBackendError(it) }
                }

                put("/core") {
                    val payload = call.receive<CoreConfigDto>()
                    runCatching { backend.saveCoreConfig(payload) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respondBackendError(it) }
                }

                get("/plugins") {
                    runCatching { backend.listPlugins() }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respondBackendError(it) }
                }

                put("/plugins/{pluginName}") {
                    val name = call.parameters["pluginName"]
                    if (name.isNullOrBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            SaveResponseDto(ok = false, message = "pluginName is required."),
                        )
                        return@put
                    }

                    val payload = call.receive<PluginToggleRequestDto>()
                    runCatching { backend.updatePluginEnabled(name, payload.enabled) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respondBackendError(it) }
                }

                get("/plugins/{pluginName}/yaml") {
                    val name = call.parameters["pluginName"]
                    if (name.isNullOrBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            SaveResponseDto(ok = false, message = "pluginName is required."),
                        )
                        return@get
                    }

                    runCatching { backend.getPluginYaml(name) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respondBackendError(it) }
                }

                put("/plugins/{pluginName}/yaml") {
                    val name = call.parameters["pluginName"]
                    if (name.isNullOrBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            SaveResponseDto(ok = false, message = "pluginName is required."),
                        )
                        return@put
                    }

                    val payload = call.receive<PluginYamlUpdateRequestDto>()
                    runCatching { backend.savePluginYaml(name, payload.yaml) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respondBackendError(it) }
                }
            }
        }

        get("/{...}") {
            val path = call.request.path()
            if (path.startsWith("/api/") || path.startsWith("/assets/") || path.startsWith("/src/")) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val indexContent = this::class.java.classLoader
                .getResource("dashboard/index.html")
                ?.readText()
                ?: """
                    <!doctype html>
                    <html>
                    <head><title>WebDashboard Frontend Missing</title></head>
                    <body>
                        <h1>Dashboard bundle not found</h1>
                        <p>Run <code>:WebDashboard:frontendBuild</code> then restart server.</p>
                    </body>
                    </html>
                """.trimIndent()

            if (indexContent.contains(DEV_ENTRY)) {
                call.setNoCacheForHtml()
                call.respondText(
                    status = HttpStatusCode.ServiceUnavailable,
                    contentType = ContentType.Text.Html,
                    text = """
                        <!doctype html>
                        <html>
                        <head><title>Frontend Bundle Invalid</title></head>
                        <body>
                            <h1>Invalid dashboard bundle</h1>
                            <p>Detected Vite dev entry <code>/src/main.tsx</code> in production bundle.</p>
                            <p>Run <code>cd Web-Dashboard/frontend && npm run build</code>, then rebuild/start backend.</p>
                        </body>
                        </html>
                    """.trimIndent()
                )
                return@get
            }

            call.setNoCacheForHtml()
            call.respondText(indexContent, ContentType.Text.Html)
        }
    }
}

private fun ApplicationCall.setNoCacheForHtml() {
    response.header(HttpHeaders.CacheControl, "no-store, no-cache, must-revalidate, max-age=0")
    response.header(HttpHeaders.Pragma, "no-cache")
}

private suspend fun ApplicationCall.respondBackendError(error: Throwable) {
    when (error) {
        is NoSuchElementException -> respond(
            HttpStatusCode.NotFound,
            SaveResponseDto(ok = false, message = error.message ?: "Resource not found.")
        )

        is IllegalArgumentException -> respond(
            HttpStatusCode.BadRequest,
            SaveResponseDto(ok = false, message = error.message ?: "Invalid request.")
        )

        is IllegalStateException -> respond(
            HttpStatusCode.Conflict,
            SaveResponseDto(ok = false, message = error.message ?: "Operation conflict.")
        )

        else -> throw error
    }
}
