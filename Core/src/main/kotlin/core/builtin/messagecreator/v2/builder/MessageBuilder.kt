package tw.xinshou.discord.core.builtin.messagecreator.v2.builder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.filedisplay.FileDisplay
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent
import net.dv8tion.jda.api.components.section.SectionContentComponent
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import tw.xinshou.discord.core.builtin.messagecreator.v1.builder.MessageBuilder as LegacyMessageBuilder
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer as LegacyMessageDataSerializer
import tw.xinshou.discord.core.builtin.messagecreator.v2.serializer.MessageDataSerializer
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.core.util.ComponentIdManager
import java.io.File

internal class MessageBuilder(
    private val messageData: MessageDataSerializer,
    private val substitutor: Substitutor = Placeholder.globalSubstitutor,
    private val componentIdManager: ComponentIdManager?,
    private val modelMapper: Map<String, Any>?,
) {
    private lateinit var builder: MessageCreateBuilder

    fun getBuilder(): MessageCreateBuilder {
        if (setupModelKeys()) return builder

        // Reuse legacy builder for content/embeds/v1 components compatibility.
        builder = LegacyMessageBuilder(
            LegacyMessageDataSerializer(
                content = messageData.content,
                embeds = messageData.embeds,
                components = messageData.components,
            ),
            substitutor,
            componentIdManager,
            modelMapper
        ).getBuilder()

        val componentsV2 = messageData.componentsV2.map(::buildTopLevelComponent)
        if (componentsV2.isNotEmpty()) {
            builder.setComponents(componentsV2)
        }

        if (messageData.isComponentsV2) {
            validateV2Message()
            builder.useComponentsV2(true)
        } else if (componentsV2.any { it.type != Component.Type.ACTION_ROW }) {
            throw IllegalArgumentException(
                "Found V2-only components in `components_v2` but `is_components_v2` is false."
            )
        }

        return builder
    }

    private fun setupModelKeys(): Boolean {
        messageData.modelKey?.let {
            val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
            builder = when (model) {
                is MessageCreateBuilder -> model
                else -> throw IllegalArgumentException("Unknown message model: $model")
            }
            return true
        }
        return false
    }

    private fun validateV2Message() {
        require(messageData.content.isBlank()) {
            "When `is_components_v2` is true, `content` must be empty."
        }
        require(messageData.embeds.isEmpty()) {
            "When `is_components_v2` is true, `embeds` are not allowed."
        }
        require(messageData.components.isNotEmpty() || messageData.componentsV2.isNotEmpty()) {
            "When `is_components_v2` is true, at least one component is required."
        }
    }

    private fun buildTopLevelComponent(data: JsonObject): MessageTopLevelComponent {
        data.getString("model_key")?.let { modelKey ->
            return resolveTopLevelModel(modelKey)
        }

        val type = data.requireString("type").normalizeType()
        val component = when (type) {
            "action_row" -> buildActionRow(data)
            "section" -> buildSection(data)
            "text_display" -> buildTextDisplay(data)
            "media_gallery" -> buildMediaGallery(data)
            "separator" -> buildSeparator(data)
            "file_display" -> buildFileDisplay(data)
            "container" -> buildContainer(data)
            else -> throw IllegalArgumentException("Unknown top-level component type: '$type'")
        }

        return applyUniqueId(component, data)
    }

    private fun buildContainer(data: JsonObject): Container {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, Container::class.java)
        }

        val children = data.getArray("components")
            .map { buildContainerChild(it.requireObject()) }

        var container = Container.of(children)
        data.getColor("accent_color")?.let { container = container.withAccentColor(it) }
        data.getBoolean("spoiler")?.let { container = container.withSpoiler(it) }
        data.getBoolean("disabled")?.let { container = container.withDisabled(it) }
        return container
    }

    private fun buildContainerChild(data: JsonObject): ContainerChildComponent {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, ContainerChildComponent::class.java)
        }

        return when (data.requireString("type").normalizeType()) {
            "action_row" -> applyUniqueId(buildActionRow(data), data)
            "section" -> applyUniqueId(buildSection(data), data)
            "text_display" -> applyUniqueId(buildTextDisplay(data), data)
            "media_gallery" -> applyUniqueId(buildMediaGallery(data), data)
            "separator" -> applyUniqueId(buildSeparator(data), data)
            "file_display" -> applyUniqueId(buildFileDisplay(data), data)
            else -> throw IllegalArgumentException("Unsupported container child type in ${data.requireString("type")}")
        }
    }

    private fun buildSection(data: JsonObject): Section {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, Section::class.java)
        }

        val accessory = buildSectionAccessory(
            data.getObject("accessory")
                ?: throw IllegalArgumentException("Section requires `accessory`.")
        )
        val content = data.getArray("components")
            .ifEmpty { data.getArray("content") }
            .map { buildSectionContent(it.requireObject()) }

        var section = Section.of(accessory, content)
        data.getBoolean("disabled")?.let { section = section.withDisabled(it) }
        return section
    }

    private fun buildSectionContent(data: JsonObject): SectionContentComponent {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, SectionContentComponent::class.java)
        }

        return when (data.requireString("type").normalizeType()) {
            "text_display" -> applyUniqueId(buildTextDisplay(data), data)
            else -> throw IllegalArgumentException("Section content only supports `text_display`.")
        }
    }

    private fun buildSectionAccessory(data: JsonObject): SectionAccessoryComponent {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, SectionAccessoryComponent::class.java)
        }

        val type = data.getString("type")?.normalizeType() ?: when {
            data.containsKey("url") || data.containsKey("file_path") || data.containsKey("file_name") -> "thumbnail"
            else -> "button"
        }

        return when (type) {
            "button" -> applyUniqueId(buildButton(data), data)
            "thumbnail" -> applyUniqueId(buildThumbnail(data), data)
            else -> throw IllegalArgumentException("Section accessory only supports `button` or `thumbnail`.")
        }
    }

    private fun buildTextDisplay(data: JsonObject): TextDisplay {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, TextDisplay::class.java)
        }

        return TextDisplay.of(parsePlaceholder(data.requireString("content")))
    }

    private fun buildMediaGallery(data: JsonObject): MediaGallery {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, MediaGallery::class.java)
        }

        val items = data.getArray("items").map { buildMediaGalleryItem(it.requireObject()) }
        return MediaGallery.of(items)
    }

    private fun buildMediaGalleryItem(data: JsonObject): MediaGalleryItem {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, MediaGalleryItem::class.java)
        }

        var item = when {
            data.containsKey("file_path") -> {
                val file = FileUpload.fromData(File(parsePlaceholder(data.requireString("file_path"))))
                MediaGalleryItem.fromFile(file)
            }

            data.containsKey("file_name") -> {
                val fileName = parsePlaceholder(data.requireString("file_name"))
                MediaGalleryItem.fromUrl("attachment://$fileName")
            }

            else -> MediaGalleryItem.fromUrl(parsePlaceholder(data.requireString("url")))
        }

        data.getString("description")?.let { item = item.withDescription(parsePlaceholder(it)) }
        data.getBoolean("spoiler")?.let { item = item.withSpoiler(it) }
        return item
    }

    private fun buildSeparator(data: JsonObject): Separator {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, Separator::class.java)
        }

        val spacing = when (data.getString("spacing")?.uppercase()) {
            "SMALL", null -> Separator.Spacing.SMALL
            "LARGE" -> Separator.Spacing.LARGE
            else -> throw IllegalArgumentException("Unknown separator spacing: ${data.getString("spacing")}")
        }
        val divider = data.getBoolean("divider") ?: true
        return Separator.create(divider, spacing)
    }

    private fun buildFileDisplay(data: JsonObject): FileDisplay {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, FileDisplay::class.java)
        }

        var fileDisplay = when {
            data.containsKey("file_path") -> {
                val file = FileUpload.fromData(File(parsePlaceholder(data.requireString("file_path"))))
                FileDisplay.fromFile(file)
            }

            else -> {
                val fileName = parsePlaceholder(data.requireString("file_name"))
                FileDisplay.fromFileName(fileName)
            }
        }

        data.getBoolean("spoiler")?.let { fileDisplay = fileDisplay.withSpoiler(it) }
        return fileDisplay
    }

    private fun buildThumbnail(data: JsonObject): Thumbnail {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, Thumbnail::class.java)
        }

        var thumbnail = when {
            data.containsKey("file_path") -> {
                val file = FileUpload.fromData(File(parsePlaceholder(data.requireString("file_path"))))
                Thumbnail.fromFile(file)
            }

            data.containsKey("file_name") -> {
                val fileName = parsePlaceholder(data.requireString("file_name"))
                Thumbnail.fromUrl("attachment://$fileName")
            }

            else -> Thumbnail.fromUrl(parsePlaceholder(data.requireString("url")))
        }

        data.getString("description")?.let { thumbnail = thumbnail.withDescription(parsePlaceholder(it)) }
        data.getBoolean("spoiler")?.let { thumbnail = thumbnail.withSpoiler(it) }
        return thumbnail
    }

    private fun buildActionRow(data: JsonObject): ActionRow {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, ActionRow::class.java)
        }

        val components = data.getArray("components").map { buildActionRowChild(it.requireObject()) }
        return ActionRow.of(components)
    }

    private fun buildActionRowChild(data: JsonObject): ActionRowChildComponent {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, ActionRowChildComponent::class.java)
        }

        return when (data.requireString("type").normalizeType()) {
            "button" -> applyUniqueId(buildButton(data), data)
            "string_select", "string_select_menu" -> applyUniqueId(buildStringSelectMenu(data), data)
            "entity_select", "entity_select_menu" -> applyUniqueId(buildEntitySelectMenu(data), data)
            else -> throw IllegalArgumentException("Unsupported action row component type: ${data.requireString("type")}")
        }
    }

    private fun buildButton(data: JsonObject): Button {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, Button::class.java)
        }

        val style = data.getButtonStyle()
        val label = data.getString("label")?.let(::parsePlaceholder)
        val emoji = data["emoji"]?.let(::parseEmoji)
        val disabled = data.getBoolean("disabled") ?: false

        val button = when (style) {
            ButtonStyle.LINK -> {
                val url = parsePlaceholder(data.requireString("url"))
                Button.of(style, url, label, emoji)
            }

            ButtonStyle.PREMIUM -> {
                val sku = data.getString("sku") ?: data.getString("sku_id")
                    ?: throw IllegalArgumentException("Premium button requires `sku` or `sku_id`.")
                Button.of(style, sku, null, null)
            }

            else -> {
                val customId = resolveCustomId(data)
                Button.of(style, customId, label, emoji)
            }
        }

        return button.withDisabled(disabled)
    }

    private fun buildStringSelectMenu(data: JsonObject): StringSelectMenu {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, StringSelectMenu::class.java)
        }

        val menu = StringSelectMenu.create(resolveCustomId(data))
        data.getString("placeholder")?.let { menu.setPlaceholder(parsePlaceholder(it)) }
        menu.setRequiredRange(data.getInt("min") ?: 1, data.getInt("max") ?: 1)
        data.getBoolean("disabled")?.let { menu.setDisabled(it) }

        val options = data.getArray("options").map { buildSelectOption(it.requireObject()) }
        menu.addOptions(options)

        if (data.containsKey("default_values")) {
            val values = data.getArray("default_values")
                .mapNotNull { it.jsonPrimitive.contentOrNull }
                .map(::parsePlaceholder)
            menu.setDefaultValues(values)
        }

        return menu.build()
    }

    private fun buildSelectOption(data: JsonObject): SelectOption {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, SelectOption::class.java)
        }

        var option = SelectOption.of(
            parsePlaceholder(data.requireString("label")),
            parsePlaceholder(data.requireString("value"))
        )
        data.getString("description")?.let { option = option.withDescription(parsePlaceholder(it)) }
        data.getBoolean("default")?.let { option = option.withDefault(it) }
        data["emoji"]?.let { option = option.withEmoji(parseEmoji(it)) }
        return option
    }

    private fun buildEntitySelectMenu(data: JsonObject): EntitySelectMenu {
        data.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, EntitySelectMenu::class.java)
        }

        val targets = when {
            data.containsKey("select_targets") -> data.getArray("select_targets").map {
                EntitySelectMenu.SelectTarget.valueOf(it.jsonPrimitive.content.uppercase())
            }

            data.containsKey("select_target_type") -> listOf(
                EntitySelectMenu.SelectTarget.valueOf(data.requireString("select_target_type").uppercase())
            )

            else -> throw IllegalArgumentException("Entity select menu requires `select_targets`.")
        }

        val menu = EntitySelectMenu.create(resolveCustomId(data), targets)
        data.getString("placeholder")?.let { menu.setPlaceholder(parsePlaceholder(it)) }
        menu.setRequiredRange(data.getInt("min") ?: 1, data.getInt("max") ?: 1)
        data.getBoolean("disabled")?.let { menu.setDisabled(it) }

        if (data.containsKey("channel_types")) {
            val channelTypes = data.getArray("channel_types")
                .map { ChannelType.valueOf(it.jsonPrimitive.content.uppercase()) }
            menu.setChannelTypes(channelTypes)
        }

        if (data.containsKey("default_values")) {
            val defaultValues = data.getArray("default_values").map { element ->
                val defaultObject = element.requireObject()
                val id = defaultObject.requireString("id")
                when (defaultObject.requireString("type").uppercase()) {
                    "USER" -> EntitySelectMenu.DefaultValue.user(id)
                    "ROLE" -> EntitySelectMenu.DefaultValue.role(id)
                    "CHANNEL" -> EntitySelectMenu.DefaultValue.channel(id)
                    else -> throw IllegalArgumentException("Unknown entity select default type: ${defaultObject.requireString("type")}")
                }
            }
            menu.setDefaultValues(defaultValues)
        }

        return menu.build()
    }

    private fun resolveCustomId(data: JsonObject): String {
        data.getString("custom_id")?.let { return parsePlaceholder(it) }

        val uid = data.getObject("uid")
            ?: throw IllegalArgumentException("Missing `custom_id` or `uid`.")
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message components with `uid`." }
        return componentIdManager.build(substitutor, uid.toFieldMap())
    }

    private fun parseEmoji(element: JsonElement): Emoji {
        if (element is JsonPrimitive) {
            val value = parsePlaceholder(element.content)
            return runCatching { Emoji.fromFormatted(value) }.getOrElse { Emoji.fromUnicode(value) }
        }

        val obj = element.requireObject()
        obj.getString("model_key")?.let { modelKey ->
            return resolveModel(modelKey, Emoji::class.java)
        }

        val raw = obj.getString("formatted")
            ?: obj.getString("name")
            ?: throw IllegalArgumentException("Emoji object requires `formatted` or `name`.")
        val value = parsePlaceholder(raw)
        return runCatching { Emoji.fromFormatted(value) }.getOrElse { Emoji.fromUnicode(value) }
    }

    private fun parsePlaceholder(text: String): String = substitutor.parse(text)

    private fun JsonObject.toFieldMap(): Map<String, Any> {
        return entries.associate { (key, value) ->
            key to when {
                value is JsonPrimitive && value.isString -> parsePlaceholder(value.content)
                value is JsonPrimitive -> value.intOrNull ?: value.longOrNull ?: value.doubleOrNull ?: value.content
                else -> value.toString()
            }
        }
    }

    private fun JsonObject.getObject(key: String): JsonObject? = this[key]?.requireObject()

    private fun JsonObject.getArray(key: String): JsonArray =
        this[key]?.jsonArray ?: JsonArray(emptyList())

    private fun JsonElement.requireObject(): JsonObject = this.jsonObject

    private fun JsonObject.getString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.requireString(key: String): String =
        getString(key) ?: throw IllegalArgumentException("Missing required field `$key`.")

    private fun JsonObject.getBoolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.getInt(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.getColor(key: String): Int? {
        val raw = this[key]?.jsonPrimitive?.contentOrNull ?: return null
        return when {
            raw.startsWith("#") || raw.startsWith("0x", ignoreCase = true) -> Integer.decode(raw)
            else -> raw.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid color format for `$key`: $raw")
        }
    }

    private fun JsonObject.getButtonStyle(): ButtonStyle {
        val element = this["style"] ?: return ButtonStyle.PRIMARY
        val primitive = element.jsonPrimitive

        primitive.intOrNull?.let {
            return when (it) {
                1 -> ButtonStyle.PRIMARY
                2 -> ButtonStyle.SECONDARY
                3 -> ButtonStyle.SUCCESS
                4 -> ButtonStyle.DANGER
                5 -> ButtonStyle.LINK
                6 -> ButtonStyle.PREMIUM
                else -> throw IllegalArgumentException("Unknown button style code: $it")
            }
        }

        return when (primitive.content.uppercase()) {
            "PRIMARY", "BLUE" -> ButtonStyle.PRIMARY
            "SECONDARY", "GRAY", "GREY" -> ButtonStyle.SECONDARY
            "SUCCESS", "GREEN" -> ButtonStyle.SUCCESS
            "DANGER", "RED" -> ButtonStyle.DANGER
            "LINK" -> ButtonStyle.LINK
            "PREMIUM" -> ButtonStyle.PREMIUM
            else -> throw IllegalArgumentException("Unknown button style: ${primitive.content}")
        }
    }

    private fun String.normalizeType(): String = lowercase().replace('-', '_')

    private fun resolveTopLevelModel(modelKey: String): MessageTopLevelComponent {
        val model = requireNotNull(modelMapper?.get(modelKey)) { "Model with key '$modelKey' not found!" }

        return when (model) {
            is MessageTopLevelComponent -> model
            is MessageEmbed -> convertLegacyEmbedModel(model)
            else -> throw IllegalArgumentException(
                "Unknown top-level model type for key '$modelKey': ${model::class.java.simpleName}"
            )
        }
    }

    private fun convertLegacyEmbedModel(embed: MessageEmbed): MessageTopLevelComponent {
        val children = mutableListOf<ContainerChildComponent>()

        val headerLines = mutableListOf<String>()
        embed.author?.name?.let { name ->
            val text = embed.author?.url?.takeIf { it.isNotBlank() }?.let { "[$name]($it)" } ?: name
            headerLines += "### $text"
        }
        embed.title?.takeIf { it.isNotBlank() }?.let { title ->
            val text = embed.url?.takeIf { it.isNotBlank() }?.let { "[$title]($it)" } ?: title
            headerLines += "## $text"
        }
        embed.description?.takeIf { it.isNotBlank() }?.let { headerLines += it }
        val header = headerLines.joinToString("\n")

        val thumbnailUrl = embed.thumbnail?.url ?: embed.author?.iconUrl
        if (thumbnailUrl != null) {
            children += Section.of(
                Thumbnail.fromUrl(thumbnailUrl),
                listOf(TextDisplay.of(if (header.isNotBlank()) header else "_ _"))
            )
        } else if (header.isNotBlank()) {
            children += TextDisplay.of(header)
        }

        embed.fields.forEach { field ->
            val name = field.name?.trim().orEmpty()
            val value = field.value?.trim().orEmpty()

            val content = when {
                name.isNotEmpty() && value.isNotEmpty() -> "**$name**\n$value"
                name.isNotEmpty() -> "**$name**"
                value.isNotEmpty() -> value
                else -> ""
            }

            if (content.isNotEmpty()) {
                children += TextDisplay.of(content)
            }
        }

        embed.image?.url?.let { imageUrl ->
            children += MediaGallery.of(
                listOf(
                    MediaGalleryItem.fromUrl(imageUrl)
                )
            )
        }

        val footerBits = mutableListOf<String>()
        embed.footer?.text?.takeIf { it.isNotBlank() }?.let { footerBits += it }
        embed.timestamp?.let { footerBits += it.toString() }
        if (footerBits.isNotEmpty()) {
            if (children.isNotEmpty()) {
                children += Separator.create(true, Separator.Spacing.SMALL)
            }
            children += TextDisplay.of("*${footerBits.joinToString(" • ")}*")
        }

        if (children.isEmpty()) {
            children += TextDisplay.of("_ _")
        }

        var container = Container.of(children)
        embed.color?.rgb?.let { container = container.withAccentColor(it) }
        return container
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> resolveModel(modelKey: String, clazz: Class<T>): T {
        val model = requireNotNull(modelMapper?.get(modelKey)) { "Model with key '$modelKey' not found!" }
        require(clazz.isInstance(model)) {
            "Unknown model type for key '$modelKey': expected ${clazz.simpleName}, actual ${model::class.java.simpleName}"
        }
        return model as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> applyUniqueId(component: T, data: JsonObject): T where T : Component {
        val uniqueId = data.getInt("id") ?: return component
        return component.withUniqueId(uniqueId) as T
    }
}
