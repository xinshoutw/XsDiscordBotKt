package tw.xinshou.discord.core.util

import java.io.File
import java.io.InputStream
import java.net.URISyntaxException
import java.util.jar.JarFile

class FileGetter(
    val pluginDirFile: File,
    private val clazz: Class<*>,
) {
    /**
     * Returns an [InputStream] for the given file name.
     * If the file already exists on disk, reads from there;
     * otherwise exports it from the JAR classpath first.
     */
    fun readOrExportInputStream(fileName: String): InputStream {
        val file = pluginDirFile.resolve(fileName)
        if (file.exists()) return file.inputStream()
        export(fileName, pluginDirFile)
        return file.inputStream()
    }

    /**
     * Exports a resource (file or directory) from the JAR classpath to a destination on disk.
     *
     * @param resourcePath path inside the JAR to export
     * @param destination  target directory on disk (defaults to [pluginDirFile])
     * @param replace      if `true`, overwrite existing files
     */
    fun export(resourcePath: String, destination: File? = pluginDirFile, replace: Boolean = false) {
        val dest = destination ?: pluginDirFile
        val jarPath = getJarPath() ?: run {
            // Fallback: running from classes (IDE), use classLoader resource stream
            exportFromClasspath(resourcePath, dest, replace)
            return
        }

        JarFile(jarPath).use { jar ->
            val normalizedPath = resourcePath.trimStart('/')
            val entries = jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith(normalizedPath) }
                .toList()

            if (entries.isEmpty()) {
                // Try as a single file resource
                exportFromClasspath(resourcePath, dest, replace)
                return
            }

            for (entry in entries) {
                val relativeName = entry.name.removePrefix(normalizedPath).trimStart('/')
                val targetFile = if (relativeName.isEmpty()) {
                    dest.resolve(File(normalizedPath).name)
                } else {
                    dest.resolve(relativeName)
                }

                if (targetFile.exists() && !replace) continue

                targetFile.parentFile?.mkdirs()
                jar.getInputStream(entry).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun exportFromClasspath(resourcePath: String, dest: File, replace: Boolean) {
        val normalizedPath = resourcePath.trimStart('/')
        val stream = clazz.classLoader.getResourceAsStream(normalizedPath) ?: return
        val targetFile = dest.resolve(File(normalizedPath).name)

        if (targetFile.exists() && !replace) {
            stream.close()
            return
        }

        targetFile.parentFile?.mkdirs()
        stream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun getJarPath(): File? {
        return try {
            val location = clazz.protectionDomain.codeSource?.location ?: return null
            val file = File(location.toURI())
            if (file.isFile && file.name.endsWith(".jar")) file else null
        } catch (_: URISyntaxException) {
            null
        }
    }
}
