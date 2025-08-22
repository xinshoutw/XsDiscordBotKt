package tw.xinshou.core.localizations

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import net.dv8tion.jda.api.interactions.DiscordLocale
import okhttp3.internal.toImmutableMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Manages language settings for Discord interaction localization.
 * @param D the type of localized data class that hold single localized language strings.
 * @param pluginDirFile used to access language file name in `./lang/%ZONE%/` resources path.
 * @param defaultLocale the default locale to be used when no locale-specific data is available.
 * @param clazzSerializer the class type of the single localized data.
 */
@OptIn(InternalSerializationApi::class)
class StringLocalizer<D : Any>(
    private val pluginDirFile: File,
    private val clazzSerializer: KClass<D>,
    private val defaultLocale: DiscordLocale = DiscordLocale.ENGLISH_US,
    private val fileName: String = "register.yaml",
) {
    private val localizations = mutableMapOf<String, LocalStringMap>()
    private var hasDefaultLocale = false // will be changed later
//    private val yaml = Yaml(SerializersModule { contextual(clazzSerializer, clazzSerializer.serializer()) })

    init {
        File(pluginDirFile, "lang").listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            val registerFile = File(directory, fileName)
            if (!registerFile.exists()) return@forEach // 如果該語言目錄下沒有指定檔案，則跳過

            val locale = DiscordLocale.from(directory.name.replace("\\.\\w+$".toRegex(), ""))
            if (locale == DiscordLocale.UNKNOWN) {
                logger.warn("Cannot identify Discord locale from file: {}", registerFile.canonicalPath)
                return@forEach
            }

            if (locale == defaultLocale) {
                hasDefaultLocale = true
            }

            try {
                registerFile.inputStream().use { inputStream ->
                    val decodedData: D = Yaml().decodeFromStream(clazzSerializer.serializer(), inputStream)
                    val flattenedMap = parseToMap(decodedData)

                    flattenedMap.forEach { (key, value) ->
                        val localStringMap = localizations.getOrPut(key) { LocalStringMap() }
                        localStringMap[locale] = value

                        if (locale == defaultLocale) {
                            localStringMap.setDefaultLocale(defaultLocale)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error decoding data for locale $locale from file: {}", registerFile.name, e)
            }
        }

        check(hasDefaultLocale) { "Default locale not found in the provided language files." }
    }

    private fun parseToMap(obj: Any): Map<String, String> {
        val result = mutableMapOf<String, String>() // Only store String results

        fun exploreFields(obj: Any, parentPrefix: String) {
            obj::class.memberProperties.forEach { property ->
                val fieldName = if (parentPrefix.isEmpty()) property.name else "$parentPrefix.${property.name}"
                try {
                    val field = obj.javaClass.getDeclaredField(property.name)
                    field.isAccessible = true
                    val value = field.get(obj)
                    if (value is String) {
                        result[fieldName] = value
                    } else if (value != null) {
                        // Only explore further if the value is not a primitive or String (assumes custom types are complex)
                        if (!field.type.isPrimitive && field.type != String::class.java) {
                            exploreFields(value, fieldName)
                        }
                    }
                } catch (e: NoSuchFieldException) {
                    logger.error("Field not found: {}", property.name, e)
                } catch (e: IllegalAccessException) {
                    logger.error("Access to field denied: {}", property.name, e)
                }
            }
        }

        exploreFields(obj, "")
        return result.toImmutableMap()
    }

    fun getLocaleData(key: String): LocalStringMap {
        return localizations[key]!!
    }

    fun get(key: String, locale: DiscordLocale): String {
        return localizations[key]!!.get(locale)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
