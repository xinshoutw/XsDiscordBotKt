package core.i18n

import com.charleskorn.kaml.Yaml
import core.placeholder.Substitutor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu.SelectTarget
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.File

class MessageTemplate(
    private val langDir: File,
    private val defaultLocale: DiscordLocale,
    private val componentIdPrefix: String? = null,
) {
    private val logger = LoggerFactory.getLogger(MessageTemplate::class.java)
    private val messages: Map<DiscordLocale, Map<String, MessageData>> = loadMessages()

    private fun loadMessages(): Map<DiscordLocale, Map<String, MessageData>> {
        val result = mutableMapOf<DiscordLocale, MutableMap<String, MessageData>>()

        if (!langDir.isDirectory) {
            logger.warn("Language directory does not exist: {}", langDir.absolutePath)
            return result
        }

        langDir.listFiles()?.filter { it.isDirectory }?.forEach { localeDir ->
            val locale = parseLocale(localeDir.name) ?: run {
                logger.warn("Unknown locale directory: {}", localeDir.name)
                return@forEach
            }

            val messageDir = File(localeDir, "message")
            if (!messageDir.isDirectory) return@forEach

            val localeMessages = result.getOrPut(locale) { mutableMapOf() }

            messageDir.listFiles()
                ?.filter { it.extension in listOf("yml", "yaml") }
                ?.forEach { file ->
                    try {
                        val messageId = file.nameWithoutExtension
                        val data = Yaml.default.decodeFromString(MessageData.serializer(), file.readText())
                        localeMessages[messageId] = data
                        logger.debug("Loaded message '{}' for locale {}", messageId, locale)
                    } catch (e: Exception) {
                        logger.error("Failed to parse message file {}: {}", file.absolutePath, e.message)
                    }
                }
        }

        return result
    }

    private fun parseLocale(tag: String): DiscordLocale? {
        return try {
            DiscordLocale.from(tag)
        } catch (_: Exception) {
            try {
                DiscordLocale.from(tag.replace('_', '-'))
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun resolveMessageData(messageId: String, locale: DiscordLocale?): MessageData? {
        val effectiveLocale = locale ?: defaultLocale
        return messages[effectiveLocale]?.get(messageId)
            ?: messages[defaultLocale]?.get(messageId)
            ?: messages.values.firstNotNullOfOrNull { it[messageId] }
    }

    fun buildCreate(
        messageId: String,
        locale: DiscordLocale? = null,
        substitutor: Substitutor? = null,
    ): MessageCreateBuilder {
        val data = resolveMessageData(messageId, locale)
            ?: throw IllegalArgumentException("Message not found: $messageId")

        val builder = MessageCreateBuilder()

        data.content?.let { builder.setContent(substitutor.apply(it)) }

        if (data.embeds.isNotEmpty()) {
            builder.setEmbeds(data.embeds.map { buildEmbed(it, substitutor) })
        }

        if (data.components.isNotEmpty()) {
            builder.setComponents(data.components.map { buildActionRow(it, substitutor) })
        }

        return builder
    }

    fun buildEdit(
        messageId: String,
        locale: DiscordLocale? = null,
        substitutor: Substitutor? = null,
    ): MessageEditBuilder {
        val data = resolveMessageData(messageId, locale)
            ?: throw IllegalArgumentException("Message not found: $messageId")

        val builder = MessageEditBuilder()

        data.content?.let { builder.setContent(substitutor.apply(it)) }

        if (data.embeds.isNotEmpty()) {
            builder.setEmbeds(data.embeds.map { buildEmbed(it, substitutor) })
        }

        if (data.components.isNotEmpty()) {
            builder.setComponents(data.components.map { buildActionRow(it, substitutor) })
        }

        return builder
    }

    private fun buildEmbed(data: EmbedData, substitutor: Substitutor?): net.dv8tion.jda.api.entities.MessageEmbed {
        val builder = EmbedBuilder()

        data.title?.let { builder.setTitle(substitutor.apply(it)) }
        data.description?.let { builder.setDescription(substitutor.apply(it)) }
        data.color?.let { builder.setColor(Color.decode(it)) }
        data.thumbnail?.let { builder.setThumbnail(substitutor.apply(it)) }
        data.image?.let { builder.setImage(substitutor.apply(it)) }

        data.footer?.let {
            builder.setFooter(substitutor.apply(it.text), it.iconUrl?.let { url -> substitutor.apply(url) })
        }

        data.author?.let {
            builder.setAuthor(
                substitutor.apply(it.name),
                it.url?.let { url -> substitutor.apply(url) },
                it.iconUrl?.let { url -> substitutor.apply(url) },
            )
        }

        data.fields.forEach {
            builder.addField(substitutor.apply(it.name), substitutor.apply(it.value), it.inline)
        }

        return builder.build()
    }

    private fun buildActionRow(data: ActionRowData, substitutor: Substitutor?): ActionRow {
        return when {
            data.buttons != null -> {
                ActionRow.of(data.buttons.map { buildButton(it, substitutor) })
            }

            data.stringSelect != null -> {
                ActionRow.of(buildStringSelect(data.stringSelect, substitutor))
            }

            data.entitySelect != null -> {
                ActionRow.of(buildEntitySelect(data.entitySelect, substitutor))
            }

            else -> throw IllegalArgumentException("ActionRow must contain buttons, string_select, or entity_select")
        }
    }

    private fun buildButton(data: ButtonData, substitutor: Substitutor?): Button {
        val label = substitutor.apply(data.label)
        val emoji = data.emoji?.let { Emoji.fromFormatted(substitutor.apply(it)) }

        val style = when (data.style.uppercase()) {
            "PRIMARY" -> ButtonStyle.PRIMARY
            "SECONDARY" -> ButtonStyle.SECONDARY
            "SUCCESS" -> ButtonStyle.SUCCESS
            "DANGER" -> ButtonStyle.DANGER
            "LINK" -> ButtonStyle.LINK
            else -> ButtonStyle.PRIMARY
        }

        val button = if (style == ButtonStyle.LINK) {
            Button.link(substitutor.apply(data.url ?: ""), label)
        } else {
            val customId = buildComponentId(substitutor.apply(data.id ?: ""))
            Button.of(style, customId, label)
        }

        val withEmoji = if (emoji != null) button.withEmoji(emoji) else button
        return if (data.disabled) withEmoji.asDisabled() else withEmoji
    }

    private fun buildStringSelect(data: StringSelectData, substitutor: Substitutor?): StringSelectMenu {
        val customId = buildComponentId(substitutor.apply(data.id))
        val builder = StringSelectMenu.create(customId)
            .setRequiredRange(data.minValues, data.maxValues)

        data.placeholder?.let { builder.setPlaceholder(substitutor.apply(it)) }

        data.options.forEach { option ->
            val optBuilder = net.dv8tion.jda.api.interactions.components.selections.SelectOption
                .of(substitutor.apply(option.label), substitutor.apply(option.value))

            var opt = optBuilder
            option.description?.let { opt = opt.withDescription(substitutor.apply(it)) }
            option.emoji?.let { opt = opt.withEmoji(Emoji.fromFormatted(substitutor.apply(it))) }
            if (option.default) opt = opt.withDefault(true)

            builder.addOptions(opt)
        }

        return builder.build()
    }

    private fun buildEntitySelect(data: EntitySelectData, substitutor: Substitutor?): EntitySelectMenu {
        val customId = buildComponentId(substitutor.apply(data.id))
        val targets = data.types.map { type ->
            when (type.uppercase()) {
                "USER" -> SelectTarget.USER
                "ROLE" -> SelectTarget.ROLE
                "CHANNEL" -> SelectTarget.CHANNEL
                else -> SelectTarget.USER
            }
        }

        val builder = EntitySelectMenu.create(customId, targets)
            .setRequiredRange(data.minValues, data.maxValues)

        data.placeholder?.let { builder.setPlaceholder(substitutor.apply(it)) }

        return builder.build()
    }

    private fun buildComponentId(id: String): String {
        return if (componentIdPrefix != null) "$componentIdPrefix:$id" else id
    }

    private fun Substitutor?.apply(text: String): String =
        this?.parse(text) ?: text
}

@Serializable
data class MessageData(
    @SerialName("model_key")
    val modelKey: String? = null,
    val content: String? = null,
    val embeds: List<EmbedData> = emptyList(),
    val components: List<ActionRowData> = emptyList(),
)

@Serializable
data class EmbedData(
    val title: String? = null,
    val description: String? = null,
    val color: String? = null,
    val footer: FooterData? = null,
    val author: AuthorData? = null,
    val thumbnail: String? = null,
    val image: String? = null,
    val fields: List<FieldData> = emptyList(),
)

@Serializable
data class FooterData(
    val text: String,
    @SerialName("icon_url")
    val iconUrl: String? = null,
)

@Serializable
data class AuthorData(
    val name: String,
    val url: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
)

@Serializable
data class FieldData(
    val name: String,
    val value: String,
    val inline: Boolean = false,
)

@Serializable
data class ActionRowData(
    val buttons: List<ButtonData>? = null,
    @SerialName("string_select")
    val stringSelect: StringSelectData? = null,
    @SerialName("entity_select")
    val entitySelect: EntitySelectData? = null,
)

@Serializable
data class ButtonData(
    val label: String,
    val style: String = "PRIMARY",
    val id: String? = null,
    val url: String? = null,
    val emoji: String? = null,
    val disabled: Boolean = false,
)

@Serializable
data class StringSelectData(
    val id: String,
    val placeholder: String? = null,
    @SerialName("min_values")
    val minValues: Int = 1,
    @SerialName("max_values")
    val maxValues: Int = 1,
    val options: List<SelectOptionData> = emptyList(),
)

@Serializable
data class SelectOptionData(
    val label: String,
    val value: String,
    val description: String? = null,
    val emoji: String? = null,
    val default: Boolean = false,
)

@Serializable
data class EntitySelectData(
    val id: String,
    val placeholder: String? = null,
    @SerialName("min_values")
    val minValues: Int = 1,
    @SerialName("max_values")
    val maxValues: Int = 1,
    val types: List<String> = listOf("USER"),
)
