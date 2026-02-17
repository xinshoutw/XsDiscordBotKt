package tw.xinshou.discord.core.builtin.messagecreator.v1.builder

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.apache.commons.lang3.StringUtils.isNumeric
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.core.util.ComponentIdManager
import java.awt.Color
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor

internal class MessageBuilder(
    private val messageData: MessageDataSerializer,
    private val substitutor: Substitutor = Placeholder.globalSubstitutor,
    private val componentIdManager: ComponentIdManager?,
    private val modelMapper: Map<String, Any>?,
) {
    private lateinit var builder: MessageCreateBuilder

    fun getBuilder(): MessageCreateBuilder {
        if (setupModelKeys()) return builder

        builder = MessageCreateBuilder()
        setupContent()
        setupEmbeds()
        setupComponents()

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

    private fun setupContent() {
        messageData.content.let {
            builder.setContent(parsePlaceholder(it))
        }
    }

    private fun setupEmbeds() {
        if (messageData.embeds.isEmpty()) return

        messageData.embeds.let { embeds ->
            builder.setEmbeds(buildEmbeds(embeds))
        }
    }

    private fun setupComponents() {
        if (messageData.components.isEmpty()) return

        messageData.components.let { components ->
            builder.setComponents(buildComponents(components))
        }
    }

    /* Process Embed Functions */
    private fun buildEmbeds(embedSettings: List<MessageDataSerializer.EmbedSetting>): List<MessageEmbed> {
        val finalEmbeds = mutableListOf<MessageEmbed>()

        embedSettings.forEach { embedSetting ->
            embedSetting.modelKey?.let {
                val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                when (model) {
                    is MessageEmbed -> finalEmbeds.add(model)
                    else -> throw IllegalArgumentException("Unknown embed model: $model")
                }
                return@forEach
            }

            buildEmbed(embedSetting)?.let { finalEmbeds.add(it) }
        }

        return finalEmbeds
    }

    private fun buildEmbed(embedSetting: MessageDataSerializer.EmbedSetting): MessageEmbed? {
        val builder = EmbedBuilder().apply {
            setupAuthor(embedSetting.author) { name, url, iconUrl -> setAuthor(name, url, iconUrl) }
            setupTitle(embedSetting.title) { title, url -> setTitle(title, url) }
            setupDescription(embedSetting.description) { setDescription(it) }
            setupThumbnailUrl(embedSetting.thumbnailUrl) { setThumbnail(it) }
            setupImage(embedSetting.imageUrl) { setImage(it) }
            setupColor(embedSetting.colorCode) { setColor(it) }
            setupFooter(embedSetting.footer) { text, iconUrl -> setFooter(text, iconUrl) }
            setupTimestamp(embedSetting.timestamp) { setTimestamp(it) }
            setupFields(
                embedSetting = embedSetting,
                addField = { name, value, inline -> addField(name, value, inline) },
                addFieldModel = { field -> addField(field) }
            )
        }

        // Build the embed only if it's not empty
        return if (builder.isEmpty) null else builder.build()
    }

    private fun setupAuthor(
        author: MessageDataSerializer.EmbedSetting.AuthorSetting?,
        setAuthor: (name: String?, url: String?, iconUrl: String?) -> EmbedBuilder
    ) = author?.let { author ->
        setAuthor(
            parsePlaceholder(author.name),
            author.url?.let { url -> parsePlaceholder(url).takeIf { it.startsWith("http") } },
            author.iconUrl?.let { url -> parsePlaceholder(url).takeIf { it.startsWith("http") } }
        )
    }

    private fun setupTitle(
        title: MessageDataSerializer.EmbedSetting.TitleSetting?,
        setTitle: (title: String?, url: String?) -> EmbedBuilder
    ) = title?.let { title ->
        setTitle(
            parsePlaceholder(title.text),

            title.url?.let { url -> parsePlaceholder(url).takeIf { it.startsWith("http") } }
        )
    }

    private fun setupDescription(
        description: String?,
        setDescription: (description: String?) -> EmbedBuilder
    ) = description?.let { setDescription(parsePlaceholder(it)) }

    private fun setupThumbnailUrl(
        thumbnailUrl: String?,
        setThumbnail: (url: String?) -> EmbedBuilder
    ) = thumbnailUrl?.let { url ->
        parsePlaceholder(url).takeIf { it.startsWith("http") }?.let(setThumbnail)
    }

    private fun setupImage(
        imageUrl: String?,
        setImage: (url: String?) -> EmbedBuilder
    ) = imageUrl?.let { url ->
        parsePlaceholder(url).takeIf { it.startsWith("http") }?.let(setImage)
    }

    private fun setupColor(
        colorCode: String?,
        setColor: (color: Color?) -> EmbedBuilder
    ) = colorCode?.let {
        setColor(
            when {
                colorCode.startsWith("%") -> Color.decode(parsePlaceholder(colorCode))
                else -> Color.decode(colorCode)
            }
        )
    }

    private fun setupFooter(
        footer: MessageDataSerializer.EmbedSetting.FooterSetting?,
        setFooter: (text: String?, iconUrl: String?) -> EmbedBuilder
    ) = footer?.let { footer ->
        setFooter(
            parsePlaceholder(footer.text),
            footer.iconUrl?.let { url -> parsePlaceholder(url).takeIf { it.startsWith("http") } }
        )
    }

    private fun setupTimestamp(
        timestamp: String?,
        setTimestamp: (timestamp: TemporalAccessor?) -> EmbedBuilder
    ) = timestamp?.let { setTimestamp(parseTimestamp(it)) }

    private fun setupFields(
        embedSetting: MessageDataSerializer.EmbedSetting,
        addField: (name: String, value: String, inline: Boolean) -> EmbedBuilder,
        addFieldModel: (field: MessageEmbed.Field?) -> EmbedBuilder,
    ) = embedSetting.fields.forEach { field ->
        field.modelKey?.let {
            val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
            when (model) {
                is MessageEmbed.Field -> addFieldModel(model)

                else -> throw IllegalArgumentException("Unknown field model: $model")
            }
            return@forEach
        }

        addField(
            parsePlaceholder(field.name),
            parsePlaceholder(field.value),
            field.inline
        )
    }


    /* Process Component Functions */
    private fun buildComponents(actionRowSettings: List<MessageDataSerializer.ActionRowSetting>): List<MessageTopLevelComponent> {
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }

        val finalActionRows = mutableListOf<MessageTopLevelComponent>()

        actionRowSettings.forEach { actionRowSetting ->
            actionRowSetting.modelKey?.let {
                val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                when (model) {
                    is MessageTopLevelComponent -> finalActionRows.add(model)
                    else -> throw IllegalArgumentException("Unknown component model: $model")
                }
                return@forEach
            }

            buildActionRow(actionRowSetting).let { finalActionRows.add(it) }
        }

        return finalActionRows
    }

    private fun buildActionRow(
        actionRowSetting: MessageDataSerializer.ActionRowSetting
    ): ActionRow {
        actionRowSetting.modelKey?.let {
            val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
            when (model) {
                is ActionRow -> return model
                else -> throw IllegalArgumentException("Unknown component model: $model")
            }
        }

        return when (actionRowSetting) {
            is MessageDataSerializer.ActionRowSetting.ButtonsSetting -> buildButtons(actionRowSetting.buttons)
            is MessageDataSerializer.ActionRowSetting.StringSelectMenuSetting -> buildStringSelectMenu(actionRowSetting)
            is MessageDataSerializer.ActionRowSetting.EntitySelectMenuSetting -> buildEntitySelectMenu(actionRowSetting)
        }
    }

    private fun buildButtons(
        buttonsSetting: List<MessageDataSerializer.ActionRowSetting.ButtonsSetting.ButtonSetting>,
    ): ActionRow {
        // Check again, IDE doesn't recognize the check in the caller function
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }

        val finalButtons = mutableListOf<Button>()

        buttonsSetting.forEach { buttonSetting ->
            buttonSetting.modelKey?.let {
                val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                when (model) {
                    is Button -> finalButtons.add(model)
                    else -> throw IllegalArgumentException("Unknown component model: $model")
                }
                return@forEach
            }

            val style = when (buttonSetting.style) {
                1 -> ButtonStyle.PRIMARY
                2 -> ButtonStyle.SECONDARY
                3 -> ButtonStyle.SUCCESS
                4 -> ButtonStyle.DANGER
                5 -> ButtonStyle.LINK
                else -> throw IllegalArgumentException("Unknown style code: ${buttonSetting.style}")
            }

            val emoji = buttonSetting.emoji?.let { emojiSetting ->
                emojiSetting.modelKey?.let {
                    val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                    when (model) {
                        is Emoji -> model
                        else -> throw IllegalArgumentException("Unknown component model: $model")
                    }
                } ?: Emoji.fromFormatted(parsePlaceholder(emojiSetting.formatted!!))
            }

            val label = buttonSetting.label?.let(::parsePlaceholder)
            val button = when (style) {
                ButtonStyle.LINK -> {
                    val url = parsePlaceholder(
                        requireNotNull(buttonSetting.url) { "Link button requires `url`!" }
                    )
                    Button.of(style, url, label, emoji)
                }

                else -> {
                    val customId = componentIdManager.build(
                        substitutor,
                        requireNotNull(buttonSetting.uid) { "Non-link button requires `uid`!" }
                    )
                    Button.of(style, customId, label, emoji)
                }
            }

            finalButtons.add(button.withDisabled(buttonSetting.disabled))
        }

        return ActionRow.of(finalButtons)
    }

    private fun buildStringSelectMenu(
        stringSelectMenuSetting: MessageDataSerializer.ActionRowSetting.StringSelectMenuSetting,
    ): ActionRow {
        // Check again, IDE doesn't recognize the check in the caller function
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }

        val menuBuilder = StringSelectMenu.create(
            parsePlaceholder(componentIdManager.build(stringSelectMenuSetting.uid))
        )
        stringSelectMenuSetting.placeholder?.let { menuBuilder.setPlaceholder(parsePlaceholder(it)) }
        menuBuilder.setRequiredRange(stringSelectMenuSetting.min, stringSelectMenuSetting.max)
        menuBuilder.addOptions(buildSelectOptions(stringSelectMenuSetting.options))

        return ActionRow.of(menuBuilder.build())
    }

    private fun buildEntitySelectMenu(
        entitySelectMenuSetting: MessageDataSerializer.ActionRowSetting.EntitySelectMenuSetting,
    ): ActionRow {
        // Check again, IDE doesn't recognize the check in the caller function
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }

        val menu = EntitySelectMenu.create(
            parsePlaceholder(componentIdManager.build(entitySelectMenuSetting.uid)),
            EntitySelectMenu.SelectTarget.valueOf(entitySelectMenuSetting.selectTargetType.uppercase())
        )
        entitySelectMenuSetting.placeholder?.let { menu.setPlaceholder(parsePlaceholder(it)) }
        menu.setRequiredRange(entitySelectMenuSetting.min, entitySelectMenuSetting.max)
        if (entitySelectMenuSetting.selectTargetType.uppercase() == "CHANNEL") {
            require(entitySelectMenuSetting.channelTypes.isNotEmpty()) {
                "'channel_types' cannot be empty when 'select_target_type' is set to 'CHANNEL'!"
            }
            menu.setChannelTypes(entitySelectMenuSetting.channelTypes.map { ChannelType.valueOf(it.uppercase()) })
        }

        return ActionRow.of(menu.build())
    }

    private fun buildSelectOptions(
        options: List<MessageDataSerializer.ActionRowSetting.StringSelectMenuSetting.OptionSetting>,
    ): List<SelectOption> {
        return options.map { option ->
            option.modelKey?.let {
                val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                when (model) {
                    is SelectOption -> return@map model
                    else -> throw IllegalArgumentException("Unknown option model: $model")
                }
            }

            SelectOption.of(
                parsePlaceholder(option.label),
                parsePlaceholder(option.value),
            ).withDescription(option.description?.let(::parsePlaceholder))
                .withDefault(option.default)
                .withEmoji(option.emoji?.let { Emoji.fromFormatted(parsePlaceholder(it.name)) })
        }
    }


    /**
     * 嘗試使用 substitutor 解析文字；若 substitutor == null，回傳原值。
     */
    private fun parsePlaceholder(text: String): String {
        return substitutor.parse(text)
    }

    /**
     * 根據文字自動判斷是否為數字 timestamp、或特殊字串 "%now%"。
     */
    private fun parseTimestamp(value: String): TemporalAccessor {
        return when {
            isNumeric(value) -> Instant.ofEpochMilli(value.toLong())
            value == "%now%" -> OffsetDateTime.now().toInstant()
            else -> throw IllegalArgumentException("Unknown format for timestamp '$value'!")
        }
    }
}
