package tw.xserver.loader.json

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
        try {
            check(file.exists()) { "File does not exist." }
            val fileText = file.readText()
            check(fileText.isNotEmpty()) { "File is empty." }
            data = Gson().fromJson(fileText, dataType)

        } catch (e: IllegalStateException) {
            logger.error("Bad format for file: {}", file.name, e)
            data = defaultFileAndData()

        } catch (e: IOException) {
            logger.error("Cannot read file.", e)
            data = defaultFileAndData()

        } catch (e: JsonSyntaxException) {
            logger.error("Bad format for file: {}", file.name, e)
            data = defaultFileAndData()
            logger.info("File {} has been reset.", file.name)

        } catch (e: Exception) {
            logger.error("Unknown error.", e)
            data = defaultFileAndData()
        }
    }

    fun <T> toClass(type: Type): T {
        return gson.fromJson(data, type)
    }

    @Synchronized
    fun save() {
        ensureNotDeleted()
        try {
            file.writeText(data.toString())
        } catch (e: IOException) {
            logger.error("Cannot save file.", e)
        }
    }

    @Synchronized
    fun delete() {
        ensureNotDeleted()
        file.delete()
        isDeleted = true
    }

    @Synchronized
    override fun close() {
        save()
    }

    private fun ensureNotDeleted() {
        if (isDeleted)
            throw IllegalStateException("Cannot perform operation: This class couldn't be used after delete method called.")
    }

    companion object {
        val gson = Gson()
        protected val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
