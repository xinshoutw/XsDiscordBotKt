package tw.xinshou.discord.core.builtin.messagecreator

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.modals.Modal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.builtin.messagecreator.builder.ModalBuilder
import tw.xinshou.discord.core.builtin.messagecreator.serializer.ModalDataSerializer
import tw.xinshou.discord.core.builtin.messagecreator.serializer.TextInputStyleSerializer
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.core.util.ComponentIdManager
import java.io.File
import java.util.*

class ModalCreator(
    langDirFile: File,
    private val componentIdManager: ComponentIdManager,
    private val defaultLocale: DiscordLocale,
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val modalLocaleMapper: MutableMap<DiscordLocale, MutableMap<String, ModalDataSerializer>> =
        EnumMap(DiscordLocale::class.java)

    init {
        val yaml = Yaml(
            serializersModule = SerializersModule {
                contextual(TextInputStyleSerializer)
            }
        )

        langDirFile.listFiles()?.filter { it.isDirectory }?.forEach { directory ->
            File(directory, "./modal/").listFiles()
                ?.filter { it.isFile && it.extension in listOf("yml", "yaml") }
                ?.forEach { file ->
                    modalLocaleMapper.getOrPut(DiscordLocale.from(directory.name)) { mutableMapOf() }[file.nameWithoutExtension] =
                        yaml.decodeFromString<ModalDataSerializer>(file.readText())

                    logger.debug(
                        "Added modal {} | {}: {}",
                        langDirFile.parentFile.nameWithoutExtension,
                        directory.name,
                        file.nameWithoutExtension
                    )
                }
        }
    }


    fun getModalBuilder(
        key: String,
        locale: DiscordLocale,
        substitutor: Substitutor = Placeholder.globalSubstitutor,
        modelMapper: Map<String, Any>? = null,
    ): Modal.Builder {
        return ModalBuilder(getModalData(key, locale), substitutor, componentIdManager, modelMapper).getBuilder()
    }

    fun getModalData(key: String, locale: DiscordLocale): ModalDataSerializer {
        return modalLocaleMapper.getOrDefault(locale, modalLocaleMapper[defaultLocale])
            ?.get(key)
            ?: throw RuntimeException("Bad modal key: $key")
    }
}
