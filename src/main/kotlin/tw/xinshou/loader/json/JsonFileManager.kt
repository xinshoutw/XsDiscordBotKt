package tw.xinshou.loader.json

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.reflect.Type

abstract class JsonFileManager<T : JsonElement>(
    private val file: File,
    private val dataType: Class<T>
) : AutoCloseable {
    private var isDeleted: Boolean = false
    protected lateinit var data: T
    protected abstract fun defaultFileAndData(): T

    init {
        initData()
    }

    @Synchronized
    private fun initData() {
        if (!file.exists()) {
            logger.debug("File {} does not exist. Creating default file.", file.absolutePath)
            data = defaultFileAndData()
            save()
            return
        }
        try {
            val fileText = file.readText()
            if (fileText.isEmpty()) {
                logger.debug("File {} is empty. Using default data.", file.absolutePath)
                data = defaultFileAndData()
                save()
            } else {
                data = gson.fromJson(fileText, dataType)
            }
        } catch (e: JsonSyntaxException) {
            logger.error("Bad JSON format in file: {}. Resetting to default data.", file.absolutePath, e)
            data = defaultFileAndData()
            save()
        } catch (e: IOException) {
            logger.error("Cannot read file: {}.", file.absolutePath, e)
            data = defaultFileAndData()
            save()
        } catch (e: Exception) {
            logger.error("Unknown error reading file: {}.", file.absolutePath, e)
            data = defaultFileAndData()
            save()
        }
    }

    fun <T> toClass(type: Type): T {
        return gson.fromJson(data, type)
    }

    @Synchronized
    fun save() {
        ensureNotDeleted()
        try {
            // 在儲存前記錄日誌
            logger.debug("Saving file: {}", file.absolutePath)
            file.writeText(data.toString())
        } catch (e: IOException) {
            logger.error("Cannot save file: {}.", file.absolutePath, e)
        }
    }

    @Synchronized
    fun delete() {
        ensureNotDeleted()
        if (file.delete()) {
            logger.debug("File {} deleted successfully.", file.absolutePath)
        } else {
            logger.error("Failed to delete file {}.", file.absolutePath)
        }
        isDeleted = true
    }

    @Synchronized
    override fun close() {
        save()
    }

    private fun ensureNotDeleted() {
        if (isDeleted)
            throw IllegalStateException("Cannot perform operation: This class cannot be used after delete() has been called.")
    }

    companion object {
        val gson = Gson()
        protected val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
