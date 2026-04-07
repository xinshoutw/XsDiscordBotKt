package core.i18n

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import net.dv8tion.jda.api.interactions.DiscordLocale
import org.slf4j.LoggerFactory
import java.io.File

class Localizer(
    private val langDir: File,
    private val defaultLocale: DiscordLocale,
) {
    private val logger = LoggerFactory.getLogger(Localizer::class.java)
    private val localizations = mutableMapOf<String, MutableMap<DiscordLocale, String>>()

    init {
        loadAll()
    }

    private fun loadAll() {
        if (!langDir.isDirectory) {
            logger.warn("Language directory does not exist: {}", langDir.absolutePath)
            return
        }

        langDir.listFiles()?.filter { it.isDirectory }?.forEach { localeDir ->
            val locale = parseLocale(localeDir.name)
            if (locale == null) {
                logger.warn("Unknown locale directory: {}", localeDir.name)
                return@forEach
            }

            val registerFile = File(localeDir, "register.yaml")
            if (!registerFile.isFile) {
                logger.debug("No register.yaml found in: {}", localeDir.absolutePath)
                return@forEach
            }

            try {
                val yamlContent = registerFile.readText()
                val parsed = Yaml.default.decodeFromString(JsonElement.serializer(), yamlContent)
                val flattened = flattenYaml(parsed)

                flattened.forEach { (key, value) ->
                    localizations.getOrPut(key) { mutableMapOf() }[locale] = value
                }

                logger.debug("Loaded {} keys for locale {} from {}", flattened.size, locale, registerFile.path)
            } catch (e: Exception) {
                logger.error("Failed to parse register.yaml in {}: {}", localeDir.absolutePath, e.message)
            }
        }
    }

    private fun flattenYaml(element: JsonElement, prefix: String = ""): Map<String, String> {
        val result = mutableMapOf<String, String>()
        when (element) {
            is JsonObject -> {
                element.forEach { (key, value) ->
                    val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
                    result.putAll(flattenYaml(value, fullKey))
                }
            }

            is JsonPrimitive -> {
                result[prefix] = element.jsonPrimitive.content
            }

            else -> {}
        }
        return result
    }

    private fun parseLocale(tag: String): DiscordLocale? {
        return try {
            DiscordLocale.from(tag)
        } catch (_: Exception) {
            // Try replacing underscore with hyphen (e.g. "zh_TW" -> "zh-TW")
            try {
                DiscordLocale.from(tag.replace('_', '-'))
            } catch (_: Exception) {
                null
            }
        }
    }

    operator fun get(key: String): LocaleMap =
        LocaleMap(localizations[key] ?: emptyMap(), defaultLocale)

    fun get(key: String, locale: DiscordLocale): String =
        this[key].resolve(locale)

    fun has(key: String): Boolean = key in localizations
}
