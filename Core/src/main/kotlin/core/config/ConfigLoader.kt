package core.config

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.serializer
import org.slf4j.LoggerFactory
import java.io.File

object ConfigLoader {
    @PublishedApi
    internal val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Load a @Serializable config from [file] using KAML.
     * If [file] does not exist and [default] resource path is provided,
     * the default will be exported from classpath before loading.
     */
    inline fun <reified T> load(file: File, default: String? = null): T {
        if (!file.exists()) {
            if (default != null) {
                exportDefault(default, file)
                logger.info("Exported default config to {}", file.absolutePath)
            } else {
                throw IllegalStateException("Config file not found: ${file.absolutePath}")
            }
        }

        val text = file.readText()
        return Yaml.default.decodeFromString(serializer<T>(), text)
    }

    /**
     * Export a classpath resource at [resourcePath] to the [targetFile].
     * Parent directories are created automatically.
     */
    fun exportDefault(resourcePath: String, targetFile: File) {
        val stream = this::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Classpath resource not found: $resourcePath")

        targetFile.parentFile?.mkdirs()
        stream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
