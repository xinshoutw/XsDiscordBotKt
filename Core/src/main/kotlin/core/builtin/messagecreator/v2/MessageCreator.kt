package tw.xinshou.discord.core.builtin.messagecreator.v2

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer.ActionRowSetting
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer.ActionRowSetting.ButtonsSetting
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer.ActionRowSetting.EntitySelectMenuSetting
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer.ActionRowSetting.StringSelectMenuSetting
import tw.xinshou.discord.core.builtin.messagecreator.v2.builder.MessageBuilder
import tw.xinshou.discord.core.builtin.messagecreator.v2.serializer.MessageDataSerializer
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.core.util.ComponentIdManager
import java.io.File
import java.util.EnumMap

class MessageCreator(
    val pluginDirFile: File,
    private val defaultLocale: DiscordLocale,
    private val componentIdManager: ComponentIdManager? = null,
    private val directoryRelativePath: String = "message/"
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val langDirFile = File(pluginDirFile, "lang")
    private val messageLocaleMapper: MutableMap<DiscordLocale, MutableMap<String, MessageDataSerializer>> =
        EnumMap(DiscordLocale::class.java)

    init {
        val yaml = Yaml(
            serializersModule = SerializersModule {
                include(SerializersModule {
                    polymorphic(ActionRowSetting::class) {
                        subclass(ButtonsSetting::class)
                        subclass(StringSelectMenuSetting::class)
                        subclass(EntitySelectMenuSetting::class)
                    }
                })
            },
            configuration = YamlConfiguration(strictMode = false)
        )

        langDirFile.listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            File(directory, directoryRelativePath).listFiles()
                ?.filter { it.isFile && it.extension in listOf("yml", "yaml") }
                ?.forEach { file ->
                    val fileContent = file.readText()
                    val componentsV2 = extractComponentsV2(yaml.parseToYamlNode(fileContent))
                    val decoded = yaml.decodeFromString<MessageDataSerializer>(fileContent)

                    messageLocaleMapper.getOrPut(DiscordLocale.from(directory.name)) { mutableMapOf() }[file.nameWithoutExtension] =
                        decoded.copy(componentsV2 = componentsV2)

                    logger.debug(
                        "Added message {} | {}: {}",
                        langDirFile.parentFile.nameWithoutExtension,
                        directory.name,
                        file.nameWithoutExtension
                    )
                }
        }
    }

    fun getCreateBuilder(
        key: String,
        locale: DiscordLocale = defaultLocale,
        substitutor: Substitutor = Placeholder.globalSubstitutor,
        modelMapper: Map<String, Any>? = null,
    ): MessageCreateBuilder =
        MessageBuilder(
            getMessageData(key, locale),
            substitutor,
            componentIdManager,
            modelMapper
        ).getBuilder()

    fun getCreateBuilder(
        key: String,
        locale: DiscordLocale = defaultLocale,
        replaceMap: Map<String, String>,
        modelMapper: Map<String, Any>? = null,
    ): MessageCreateBuilder =
        getCreateBuilder(
            key,
            locale,
            Placeholder.globalSubstitutor.putAll(replaceMap),
            modelMapper
        )

    fun getEditBuilder(
        key: String,
        locale: DiscordLocale = defaultLocale,
        substitutor: Substitutor = Placeholder.globalSubstitutor,
        modelMapper: Map<String, Any>? = null,
    ): MessageEditBuilder =
        MessageEditBuilder.fromCreateData(
            getCreateBuilder(key, locale, substitutor, modelMapper).build()
        )

    fun getEditBuilder(
        key: String,
        locale: DiscordLocale = defaultLocale,
        replaceMap: Map<String, String>,
        modelMapper: Map<String, Any>? = null,
    ): MessageEditBuilder =
        MessageEditBuilder.fromCreateData(
            getCreateBuilder(key, locale, Placeholder.globalSubstitutor.putAll(replaceMap), modelMapper).build()
        )

    fun getMessageData(key: String, locale: DiscordLocale): MessageDataSerializer {
        return messageLocaleMapper.getOrDefault(locale, messageLocaleMapper[defaultLocale])
            ?.get(key)
            ?: throw IllegalStateException("Message data not found for command: $key")
    }

    private fun extractComponentsV2(rootNode: YamlNode): List<JsonObject> {
        val root = rootNode as? YamlMap ?: return emptyList()
        val componentsNode = root.entries.entries
            .firstOrNull { (key, _) -> key.content == "components_v2" }
            ?.value
            ?: return emptyList()
        val componentsList = componentsNode as? YamlList ?: return emptyList()

        return componentsList.items.map { node ->
            val jsonElement = yamlNodeToJsonElement(node)
            require(jsonElement is JsonObject) {
                "Each `components_v2` entry must be an object."
            }
            jsonElement
        }
    }

    private fun yamlNodeToJsonElement(node: YamlNode): JsonElement {
        return when (node) {
            is YamlMap -> JsonObject(
                node.entries.entries.associate { (key, value) ->
                    key.content to yamlNodeToJsonElement(value)
                }
            )

            is YamlList -> JsonArray(node.items.map(::yamlNodeToJsonElement))
            is YamlScalar -> yamlScalarToJsonElement(node)
            else -> throw IllegalArgumentException("Unsupported yaml node type: ${node::class.java.simpleName}")
        }
    }

    private fun yamlScalarToJsonElement(node: YamlScalar): JsonElement {
        val raw = node.content
        if (raw.equals("null", ignoreCase = true) || raw == "~") return JsonNull

        raw.toBooleanStrictOrNull()?.let { return JsonPrimitive(it) }
        raw.toIntOrNull()?.let { return JsonPrimitive(it) }
        raw.toLongOrNull()?.let { return JsonPrimitive(it) }
        raw.toDoubleOrNull()?.let { return JsonPrimitive(it) }

        return JsonPrimitive(raw)
    }
}
