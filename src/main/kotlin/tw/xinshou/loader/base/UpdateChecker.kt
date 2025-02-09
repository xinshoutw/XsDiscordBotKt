package tw.xinshou.loader.base

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.logger.Color
import tw.xinshou.loader.util.Arguments.ignoreVersionCheck
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.Channels
import kotlin.system.exitProcess

internal object UpdateChecker {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private const val VERSION = "v2.0"

    fun versionCheck(): Boolean {
        if (ignoreVersionCheck) {
            logger.info("Version check ignored.")
            return false
        }

        logger.info("Checking version...")
        val client = OkHttpClient()
        var response: Response? = null

        try {
            val request = Request.Builder()
                .url("https://github.com/IceXinShou/XsDiscordBot/releases/latest").build()
            response = client.newCall(request).execute()

            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val latestVersion = response.request.url.toString().substringAfterLast('/')
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
            val downloadRequest = Request.Builder().url(downloadURL).build()
            client.newCall(downloadRequest).execute().use { fileResponse ->
                if (!fileResponse.isSuccessful) throw IOException("Unexpected code $fileResponse")

                FileOutputStream("./$fileName").use { fos ->
                    Channels.newChannel(fileResponse.body!!.byteStream()).use { inputChannel ->
                        fos.channel.transferFrom(inputChannel, 0, Long.MAX_VALUE)
                    }
                }
            }

            logger.info("Download successfully completed. Please update to the latest version.")
            return true
        } catch (e: Exception) {
            logger.error("Error checking version.", e)
            exitProcess(1)
        } finally {
            response?.close()
        }
    }
}
