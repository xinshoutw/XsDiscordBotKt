package tw.xinshou.loader.util

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
    fun readInputStream(fileName: String): InputStream {
        val file = File(pluginDirFile, fileName)
        try {
            initFile(resourceFilePath = fileName, exportFile = file)
            logger.info("Loaded file: {}.", file.canonicalPath)
            return file.inputStream()
        } catch (e: IOException) {
            logger.error("Failed to read resource: {}!", e.message)
            throw e
        }
    }

    /**
     * Exports a resource from within the JAR to the filesystem.
     *
     * @param resourceFilePath The internal path to the resource.
     * @param outputFile The File where the resource will be written to.
     * @return A File object representing the copied file.
     * @throws IOException if an I/O error occurs during the export.
     */
    @Throws(IOException::class)
    fun exportResource(resourceFilePath: String, outputFile: File = File(resourceFilePath)): File {
        val inputStream: InputStream = clazz.getResourceAsStream(resourceFilePath.removePrefix("/"))
            ?: throw FileNotFoundException("Resource not found: $resourceFilePath")
        logger.debug("Output file: {}.", outputFile.canonicalPath)
        inputStream.use { fileInJar ->
            Files.createDirectories(outputFile.parentFile.toPath()) // init directories
            Files.copy(fileInJar, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return outputFile
        }
    }

    /**
     * Lists resources located under a specific path within the JAR.
     *
     * @param path The internal path within the JAR.
     * @return An array of resource names under the specified path.
     * @throws IOException if an error occurs during resource listing.
     */
    @Throws(IOException::class)
    fun getResourceList(path: String): Array<ZipEntry> {
        val adjustedPath = path.removePrefix(".").removePrefix("/")
        val jarUrl = clazz.protectionDomain.codeSource.location
        val filenames = mutableListOf<ZipEntry>()

        jarUrl.openStream().use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry?
                while (zip.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name
                    if (entryName.startsWith(adjustedPath) && !entryName.endsWith("/")) {
                        filenames.add(entry)
                    }
                }
            }
        }

        return filenames.toTypedArray()
    }


    /**
     * Exports default language files from the resources to the language folder.
     *
     * @throws IOException if default language files are not found or cannot be exported.
     */
    @Throws(IOException::class)
    fun exportDefaultDirectory(
        resourcePath: String,
        destDirectory: File = File(pluginDirFile, resourcePath),
        forceReplace: Boolean = Arguments.forceReplaceLangResources
    ) {
        if (Arguments.forceRenewLangResources) {
            val success = destDirectory.deleteRecursively()
            if (!success) {
                logger.warn("Failed to delete directory: {}.", destDirectory.canonicalPath)
            }

            Files.createDirectories(destDirectory.toPath())
            logger.info("Directory modified: {}.", success)
        }

        val filenames = getResourceList(resourcePath)
        require(filenames.isNotEmpty()) { "No default resource files found in $resourcePath." }

        filenames.forEach { zipEntry ->
            val outputFile = File(destDirectory, zipEntry.name.removePrefix(resourcePath).removePrefix("/"))
            if (forceReplace || !outputFile.exists())
                exportResource(zipEntry.name, outputFile)
        }
    }

    /**
     * Checks if a file is available; if not, it tries to export it.
     *
     * @param resourceFilePath Export from where.
     * @param exportFile The file to check.
     * @return A File object pointing to the existing or newly created file.
     * @throws IOException if the file cannot be created.
     */
    @Throws(IOException::class)
    private fun initFile(
        resourceFilePath: String,
        exportFile: File,
        forceReplace: Boolean = false
    ) {
        if (forceReplace || !exportFile.exists()) {
            logger.info("Creating default file: {}.", exportFile.path)
            exportResource(resourceFilePath, exportFile)
        }
    }
}
