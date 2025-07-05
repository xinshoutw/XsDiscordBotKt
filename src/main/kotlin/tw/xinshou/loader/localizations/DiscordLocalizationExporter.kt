package tw.xinshou.loader.localizations

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import net.dv8tion.jda.api.interactions.DiscordLocale
import okhttp3.internal.toImmutableMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Manages language settings for Discord interaction localization.
 * @param D the type of localized data class that hold single localized language strings.
 * @param L the type of localized data class that holds all localized strings.
 * @param pluginDirFile used to access language file name in `./lang/%ZONE%/` resources path.
 * @param defaultLocale the default locale to be used when no locale-specific data is available.
 * @param clazzSerializer the class type of the single localized data.
 * @param clazzLocalization the class type of the all localized data.
 */
@OptIn(InternalSerializationApi::class)
class DiscordLocalizationExporter<D : Any, L : Any>(
    private val pluginDirFile: File,
    private val fileName: String,
    private val defaultLocale: DiscordLocale,
    private val clazzSerializer: KClass<D>,
    private val clazzLocalization: KClass<L>
) {
    private val lang: L = clazzLocalization.objectInstance ?: throw Exception("Localization must be object!")
    private var hasDefaultLocale = false // will be changed later
    private val yaml = Yaml(serializersModule = SerializersModule {
        contextual(clazzSerializer, clazzSerializer.serializer())
    })

    init {
        File(pluginDirFile, "lang").listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            val registerFile = File(directory, fileName)
            val locale = DiscordLocale.from(directory.name.replace("\\.\\w+$".toRegex(), ""))
            if (locale == DiscordLocale.UNKNOWN) {
                logger.warn("Cannot identify Discord locale from file: {}", registerFile.canonicalPath)
                return@forEach
            }

            if (locale == defaultLocale) hasDefaultLocale = true

            try {
                val decodedData: D = yaml.decodeFromString(clazzSerializer.serializer(), registerFile.readText())
                val result = parseToMap(decodedData)
                result.forEach { (path, value) ->
                    getFieldByPath(lang, path).apply {
                        if (locale == defaultLocale) setDefaultLocale(locale)
                        set(locale, value)
                    }
                }

            } catch (e: Exception) {
                logger.error("Error decoding data for locale $locale from file: {}", registerFile.name, e)
            }
        }

        if (!hasDefaultLocale)
            throw IllegalStateException("Default locale not found in the provided language files.")
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

    private fun getFieldByPath(obj: Any, path: String): LocaleData {
        var currentObj: Any? = obj
        val pathSegments = path.split('.')
        try {
            for (segment in pathSegments) {
                val field: Field = try {
                    currentObj!!.javaClass.getDeclaredField(segment)
                } catch (e: NoSuchFieldException) {
                    currentObj!!.javaClass.getField(segment)
                }.apply { isAccessible = true }

                currentObj = field.get(currentObj)

                if (currentObj is LocaleData) {
                    return currentObj
                }
            }
        } catch (e: NoSuchFieldException) {
            logger.error("Field not found in the path: {}", path, e)
        } catch (e: IllegalAccessException) {
            logger.error("Access to field denied in the path: {}", path, e)
        } catch (e: Exception) {
            logger.error("Error processing path: {}", path, e)
        }

        throw NoSuchFieldException("LocaleData not found in the end of path: $path")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
