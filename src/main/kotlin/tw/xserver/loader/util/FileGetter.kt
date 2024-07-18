package tw.xserver.loader.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Provides functionalities to interact with files and resources, supporting operations
 * such as reading, exporting, and listing files and resources, especially within JAR files.
 */
class FileGetter(folderPath: String, private val clazz: Class<*>) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FileGetter::class.java)
    }

    val dir = File(folderPath)

    init {
        if (dir.mkdirs()) {
            logger.debug("Folder created: $folderPath")
        }
    }

    /**
     * Opens an InputStream for a specific file within the designated folder.
     *
     * @param fileName The name of the file to open.
     * @return An InputStream of the file.
     * @throws IOException if the file cannot be read.
     */
    @Throws(IOException::class)
    fun readInputStream(fileName: String): InputStream {
        val file = File(dir, fileName)
        try {
            checkFileAvailable(file)
            logger.info("Loaded file: ${file.path}")
            return Files.newInputStream(file.toPath())
        } catch (e: IOException) {
            logger.error("Failed to read resource: ${e.message}")
            throw e
        }
    }

    /**
     * Exports a resource from within the JAR to the filesystem.
     *
     * @param sourceFilePath The internal path to the resource.
     * @param outputPath The path where the resource will be written to.
     * @return A File object representing the copied file.
     * @throws IOException if an I/O error occurs during the export.
     */
    @Throws(IOException::class)
    fun exportResource(sourceFilePath: String, outputPath: String = sourceFilePath): File {
        getResource(sourceFilePath).use { fileInJar ->
            val outputFile = File(dir, outputPath)
            outputFile.parentFile.mkdirs()
            Files.copy(fileInJar, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return outputFile
        }
    }

    /**
     * Retrieves a resource as an InputStream.
     *
     * @param sourceFilePath The path to the resource.
     * @return An InputStream of the resource.
     * @throws FileNotFoundException if the resource cannot be found.
     */
    @Throws(FileNotFoundException::class)
    private fun getResource(sourceFilePath: String): InputStream {
        return clazz.getResourceAsStream(sourceFilePath)
            ?: throw FileNotFoundException("Resource not found: $sourceFilePath")
    }

    /**
     * Lists resources located under a specific path within the JAR.
     *
     * @param path The internal path within the JAR.
     * @return An array of resource names under the specified path.
     * @throws IOException if an error occurs during resource listing.
     */
    @Throws(IOException::class)
    fun getResources(path: String): Array<String> {
        val adjustedPath = path.removePrefix("/")
        val filenames = mutableListOf<String>()
        val jarUrl = clazz.protectionDomain.codeSource.location

        jarUrl.openStream().use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry?
                while (zip.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name
                    if (entryName.startsWith(adjustedPath) && !entryName.endsWith("/")) {
                        filenames.add(entryName.substring(adjustedPath.length))
                    }
                }
            }
        }

        return filenames.toTypedArray()
    }

    /**
     * Checks if a file is available; if not, it tries to export it.
     *
     * @param file The file to check.
     * @return A File object pointing to the existing or newly created file.
     * @throws IOException if the file cannot be created.
     */
    @Throws(IOException::class)
    private fun checkFileAvailable(file: File) {
        if (!file.exists()) {
            logger.info("Creating default file: ${file.path}")
            exportResource(file.path)
        }
    }
}
