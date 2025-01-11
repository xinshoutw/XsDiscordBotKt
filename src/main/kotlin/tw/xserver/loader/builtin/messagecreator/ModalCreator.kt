package tw.xserver.loader.builtin.messagecreator

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.modals.Modal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.builtin.messagecreator.serializer.ModalDataSerializer
import tw.xserver.loader.builtin.messagecreator.serializer.TextInputStyleSerializer
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.loader.util.ComponentIdManager
import java.io.File
import java.util.*

class ModalCreator(
    langDirFile: File,
    componentIdManager: ComponentIdManager,
    private val defaultLocale: DiscordLocale,
    private val modalKeys: List<String>,
) : ModalBuilder(componentIdManager) {

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
    ): Modal.Builder {
        return getModalBuilder(getModalData(key, locale), substitutor)
    }

    fun getModalData(key: String, locale: DiscordLocale): ModalDataSerializer {
        if (key !in modalKeys) {
            throw IllegalStateException("Modal data not found for key: $key")
        }

        return modalLocaleMapper.getOrDefault(locale, modalLocaleMapper[defaultLocale])
            ?.get(key)
            ?: throw RuntimeException("Bad modal key: $key")
    }
}
