package tw.xinshou.discord.core.base

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.setting.SettingSerializer
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

internal object SettingsLoader {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private const val CONFIG_NAME: String = "config.yaml"
    lateinit var config: SettingSerializer
    lateinit var token: String

    @Throws(IOException::class)
    fun run() {
        var settingFile = File("./$CONFIG_NAME")
        if (!settingFile.exists()) {
            logger.info("{} not found, create default {}!", CONFIG_NAME, CONFIG_NAME)
            settingFile = exportResource()
        }

        logger.info("Loading {}.", settingFile.path)
        config = Yaml().decodeFromString<SettingSerializer>(settingFile.readText())
        token = config.generalSettings.botToken
        logger.info("Setting file loaded.")
    }

    private fun exportResource(): File {
        try {
            this@SettingsLoader.javaClass.classLoader.getResourceAsStream(CONFIG_NAME).use { fileInJar ->
                if (fileInJar == null) {
                    logger.error("Cannot find resource: {}!", CONFIG_NAME)
                    throw MissingResourceException(
                        "Cannot find resource $CONFIG_NAME!",
                        this@SettingsLoader.javaClass.classLoader.name,
                        CONFIG_NAME
                    )
                }
                Files.copy(
                    fileInJar,
                    Paths.get("./$CONFIG_NAME"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                return File("./$CONFIG_NAME")
            }
        } catch (e: IOException) {
            logger.error("Read resource failed: {}!", e.message)
            throw e
        }
    }
}