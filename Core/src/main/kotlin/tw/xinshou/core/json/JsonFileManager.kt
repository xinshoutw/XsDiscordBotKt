package tw.xinshou.core.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

/**
 * JsonFileManager 使用 Moshi 進行 JSON 讀寫管理，不再需要抽象的 defaultFileAndData() 方法，
 * 而是要求在初始化時帶入一個預設的 T 實例（defaultInstance）。
 *
 * 若預設實例 (defaultInstance) 為 null，則在初始化讀取檔案資料時遇到問題（檔案不存在、格式錯誤等）
 * 會直接拋出 IllegalStateException。
 */
class JsonFileManager<T>(
    private val file: File,
    private val adapter: JsonAdapter<T>,
    private val defaultInstance: T? = null
) : AutoCloseable {
    var isDeleted: Boolean = false
    var data: T

    init {
        data = if (!file.exists()) {
            logger.debug("File {} does not exist.", file.absolutePath)
            defaultInstance
                ?: throw IllegalStateException("File ${file.absolutePath} does not exist and no default instance provided")
        } else {
            readFileData()
        }
        // 初始化完成後將資料寫入檔案（若檔案不存在則建立檔案）
        save()
    }

    @Synchronized
    private fun readFileData(): T {
        try {
            val fileText = file.readText()
            if (fileText.isEmpty()) {
                logger.debug("File {} is empty.", file.absolutePath)
                return defaultOrThrow("File is empty and no default instance provided")
            }

            return adapter.fromJson(fileText) ?: run {
                logger.error("Adapter returned null for file: {}.", file.absolutePath)
                defaultOrThrow("Adapter returned null and no default instance provided")
            }

        } catch (e: JsonDataException) {
            logger.error("Bad JSON format in file: {}.", file.absolutePath, e)
            return defaultOrThrow("Bad JSON format and no default instance provided", e)

        } catch (e: IOException) {
            logger.error("Cannot read file: {}.", file.absolutePath, e)
            return defaultOrThrow("Cannot read file and no default instance provided", e)

        } catch (e: Exception) {
            logger.error("Unknown error reading file: {}.", file.absolutePath, e)
            return defaultOrThrow("Unknown error reading file and no default instance provided", e)
        }
    }

    /**
     * 若有預設實例，則回傳它；否則拋出 IllegalStateException。
     */
    private fun defaultOrThrow(message: String, cause: Exception? = null): T {
        return defaultInstance ?: throw IllegalStateException(message, cause)
    }

    @Synchronized
    fun save() {
        ensureNotDeleted()
        try {
            logger.debug("Saving file: {}", file.absolutePath)
            file.writeText(adapter.toJson(data))
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
        private val logger: Logger = LoggerFactory.getLogger(JsonFileManager::class.java)
        val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T> Moshi.adapterReified(): JsonAdapter<T> {
            return adapter(typeOf<T>().javaType)
        }
    }
}
