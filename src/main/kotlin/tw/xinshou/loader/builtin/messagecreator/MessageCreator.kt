package tw.xinshou.loader.builtin.messagecreator

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.builtin.messagecreator.builder.MessageBuilder
import tw.xinshou.loader.builtin.messagecreator.serializer.MessageDataSerializer
import tw.xinshou.loader.builtin.placeholder.Placeholder
import tw.xinshou.loader.builtin.placeholder.Substitutor
import tw.xinshou.loader.util.ComponentIdManager
import java.io.File
import java.util.*

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
                    polymorphic(MessageDataSerializer.ActionRowSetting::class) {
                        subclass(MessageDataSerializer.ActionRowSetting.ButtonsSetting::class)
                        subclass(MessageDataSerializer.ActionRowSetting.StringSelectMenuSetting::class)
                        subclass(MessageDataSerializer.ActionRowSetting.EntitySelectMenuSetting::class)
                    }
                })
            }
        )

        langDirFile.listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            File(directory, directoryRelativePath).listFiles()
                ?.filter { it.isFile && it.extension in listOf("yml", "yaml") }
                ?.forEach { file ->
                    messageLocaleMapper.getOrPut(DiscordLocale.from(directory.name)) { mutableMapOf() }[file.nameWithoutExtension] =
                        yaml.decodeFromString<MessageDataSerializer>(file.readText())

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
}
