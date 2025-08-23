package tw.xinshou.core.base

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.core.logger.Color
import tw.xinshou.core.util.Arguments.ignoreVersionCheck
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.channels.Channels

internal object UpdateChecker {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private const val VERSION = "v2.0.1"

    fun versionCheck(): Boolean {
        if (ignoreVersionCheck) {
            logger.info("Version check ignored.")
            return false
        }

        logger.info("Checking version...")
        val client = HttpClient.newHttpClient()

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/IceXinShou/XsDiscordBot/releases/latest"))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) throw IOException("Unexpected code ${response.statusCode()}")

            val latestVersion = response.uri().toString().substringAfterLast('/')
            val fileName = "XsDiscordBotLoader_$latestVersion.jar"
            val downloadURL = "https://github.com/IceXinShou/XsDiscordBot/releases/download/$latestVersion/$fileName"

            // Log version info
            if (VERSION == latestVersion) {
                logger.info("You are running on the latest version: {}{}{}", Color.GREEN, VERSION, Color.RESET)
                return false
            } else {
                logger.warn(
                    "Your current version: ${Color.RED}$VERSION${Color.RESET}, " +
                            "latest version: ${Color.GREEN}$latestVersion${Color.RESET}"
                )
                logger.info("Downloading latest version file...")
            }

            // Download the new version
            val downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadURL))
                .GET()
                .build()

            val fileResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream())

            if (fileResponse.statusCode() != 200) throw IOException("Unexpected code ${fileResponse.statusCode()}")

            FileOutputStream("./$fileName").use { fos ->
                Channels.newChannel(fileResponse.body()).use { inputChannel ->
                    fos.channel.transferFrom(inputChannel, 0, Long.MAX_VALUE)
                }
            }

            logger.info("Download successfully completed. Please update to the latest version.")
            return true
        } catch (e: Exception) {
            logger.error("Error checking version.", e)
            logger.error("Please check your network connection or try again later.")
            throw e
        }
    }
}