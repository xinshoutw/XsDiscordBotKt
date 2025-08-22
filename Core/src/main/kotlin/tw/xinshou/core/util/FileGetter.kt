package tw.xinshou.core.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.JarURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileGetter(private val pluginDirFile: File, private val clazz: Class<*>) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    init {
        Files.createDirectories(pluginDirFile.toPath())
    }

    /**
     * Opens an InputStream for a specific file within the designated folder.
     *
     * @param fileName The name of the file to open.
     * @return An InputStream of the file.
     * @throws IOException if the file cannot be read.
     */
    @Throws(IOException::class)
    fun readOrExportInputStream(fileName: String): InputStream {
        val file = File(pluginDirFile, fileName)

        try {
            if (!file.exists()) {
                logger.info("The file is not found, try to export from resources: {}.", file.name)
                export(fileName, file)
            }

            logger.info("Loaded file: {}.", file.canonicalPath)
            return file.inputStream()
        } catch (e: IOException) {
            logger.error("Failed to read resource: {}!", e.message)
            throw e
        }
    }

    /**
     * Exports a resource or a directory of resources from within the JAR/classpath to the filesystem.
     * This method is optimized to handle both single files and directories efficiently.
     *
     * @param resourcePath The internal path to the resource or directory.
     * For a directory, it's recommended to end with a '/'. Ex: "lang/".
     * For a file, the path should be exact. Ex: "config.yml".
     * @param destination The destination file or directory.
     * If null, it defaults to a file or directory with the same name inside `pluginDirFile`.
     * @param replace If true, existing files will be overwritten.
     * @throws IOException if an I/O error occurs.
     * @throws FileNotFoundException if the base resource path does not exist.
     */
    @Throws(IOException::class)
    fun export(resourcePath: String, destination: File? = null, replace: Boolean = false) {
        val cleanPath = resourcePath.removePrefix("/").let { it.ifEmpty { "" } }
        val resourceUrl = clazz.getResource(cleanPath)
            ?: throw FileNotFoundException("Resource path not found: $cleanPath")

        val jarConnection = resourceUrl.openConnection()

        if (jarConnection !is JarURLConnection) {
            logger.error("Export target is not in a JAR. Skipping export for resource: {}", resourceUrl)
            assert(false)
        }

        (jarConnection as JarURLConnection).jarFile.use { jarFile ->
            val entries = jarFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.name.startsWith(cleanPath)) {
                    continue
                }

                val relativePath = entry.name.removePrefix(cleanPath).removePrefix("/")
                val destFile = when {
                    destination != null && cleanPath == entry.name -> destination
                    destination != null -> File(destination, relativePath)
                    else -> File(pluginDirFile, entry.name)
                }

                if (entry.isDirectory) {
                    Files.createDirectories(destFile.toPath())
                } else {
                    if (relativePath.isEmpty() && destFile.isDirectory) continue
                    if (destFile.exists() && !replace) continue

                    Files.createDirectories(destFile.parentFile.toPath())
                    jarFile.getInputStream(entry).use { input ->
                        Files.copy(input, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }


//        logger.info("Exporting resources from filesystem for path: {}", cleanPath)
//        val sourceFile = File(resourceUrl.toURI())
//        val baseDest = destination ?: File(pluginDirFile, cleanPath)
//
//        sourceFile.walkTopDown().forEach { file ->
//            val relativePath = file.toRelativeString(sourceFile)
//            val destFile = File(baseDest, relativePath)
//
//            if (file.isDirectory) {
//                Files.createDirectories(destFile.toPath())
//            } else {
//                if (destFile.exists() && !replace) {
//                    return@forEach // continue
//                }
//                Files.createDirectories(destFile.parentFile.toPath())
//                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
//            }
//        }
    }
}
