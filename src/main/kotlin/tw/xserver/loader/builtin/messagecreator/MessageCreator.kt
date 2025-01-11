package tw.xserver.loader.builtin.messagecreator

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.builtin.messagecreator.serializer.ColorSerializer
import tw.xserver.loader.builtin.messagecreator.serializer.MessageDataSerializer
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.loader.util.ComponentIdManager
import java.io.File
import java.util.*

class MessageCreator(
    langDirFile: File,
    private val defaultLocale: DiscordLocale,
    private val messageKeys: List<String>,
    componentIdManager: ComponentIdManager? = null,
) : MessageBuilder(componentIdManager) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val messageLocaleMapper: MutableMap<DiscordLocale, MutableMap<String, MessageDataSerializer>> =
        EnumMap(DiscordLocale::class.java)

    init {
        val yaml = Yaml(
            serializersModule = SerializersModule {
                contextual(ColorSerializer)
                include(SerializersModule {
                    polymorphic(MessageDataSerializer.ComponentSetting::class) {
                        subclass(MessageDataSerializer.ComponentSetting.ButtonsComponent::class)
                        subclass(MessageDataSerializer.ComponentSetting.StringSelectMenuSetting::class)
                        subclass(MessageDataSerializer.ComponentSetting.EntitySelectMenuSetting::class)
                    }
                })
            }
        )

        langDirFile.listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            File(directory, "./message/").listFiles()
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
    ): MessageCreateBuilder {
        return getCreateBuilder(getMessageData(key, locale), substitutor)
    }

    fun getMessageData(key: String, locale: DiscordLocale, fuzzy: Boolean = false): MessageDataSerializer {
        if (key !in messageKeys) {
            if (fuzzy)
                return getMessageData(key.replace('_', '-'), locale)
            throw IllegalStateException("Message data not found for key: $key")
        }

        return messageLocaleMapper.getOrDefault(locale, messageLocaleMapper[defaultLocale])
            ?.get(key)
            ?: throw IllegalStateException("Message data not found for command: $key")
    }
}
