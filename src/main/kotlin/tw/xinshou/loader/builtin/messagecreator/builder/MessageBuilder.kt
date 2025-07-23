package tw.xinshou.loader.builtin.messagecreator.builder

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.apache.commons.lang3.StringUtils.isNumeric
import tw.xinshou.loader.builtin.messagecreator.serializer.MessageDataSerializer
import tw.xinshou.loader.builtin.messagecreator.serializer.MessageDataSerializer.ActionRowSetting.*
import tw.xinshou.loader.builtin.messagecreator.serializer.MessageDataSerializer.ActionRowSetting.ButtonsSetting.ButtonSetting
import tw.xinshou.loader.builtin.messagecreator.serializer.MessageDataSerializer.ActionRowSetting.StringSelectMenuSetting.OptionSetting
import tw.xinshou.loader.builtin.messagecreator.serializer.MessageDataSerializer.EmbedSetting
import tw.xinshou.loader.builtin.placeholder.Placeholder
import tw.xinshou.loader.builtin.placeholder.Substitutor
import tw.xinshou.loader.util.ComponentIdManager
import java.awt.Color
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor

internal class MessageBuilder(
    private val messageData: MessageDataSerializer,
    private val substitutor: Substitutor? = Placeholder.globalSubstitutor,
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
    private fun buildEmbeds(embedSettings: List<EmbedSetting>): List<MessageEmbed> {
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

    private fun buildEmbed(embedSetting: EmbedSetting): MessageEmbed? {
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
        author: EmbedSetting.AuthorSetting?,
        setAuthor: (name: String?, url: String?, iconUrl: String?) -> EmbedBuilder
    ) = author?.let { author ->
        setAuthor(
            parsePlaceholder(author.name),

            author.url?.let { parsePlaceholder(it) },
            author.iconUrl?.let { parsePlaceholder(it) }
        )
    }

    private fun setupTitle(
        title: EmbedSetting.TitleSetting?,
        setTitle: (title: String?, url: String?) -> EmbedBuilder
    ) = title?.let { title ->
        setTitle(
            parsePlaceholder(title.text),

            title.url?.let { parsePlaceholder(it) }
        )
    }

    private fun setupDescription(
        description: String?,
        setDescription: (description: String?) -> EmbedBuilder
    ) = description?.let { setDescription(parsePlaceholder(it)) }

    private fun setupThumbnailUrl(
        thumbnailUrl: String?,
        setThumbnail: (url: String?) -> EmbedBuilder
    ) = thumbnailUrl?.let { setThumbnail(parsePlaceholder(it)) }

    private fun setupImage(
        imageUrl: String?,
        setImage: (url: String?) -> EmbedBuilder
    ) = imageUrl?.let { setImage(parsePlaceholder(it)) }

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
        footer: EmbedSetting.FooterSetting?,
        setFooter: (text: String?, iconUrl: String?) -> EmbedBuilder
    ) = footer?.let { footer ->
        setFooter(
            parsePlaceholder(footer.text),
            footer.iconUrl?.let { parsePlaceholder(it) }
        )
    }

    private fun setupTimestamp(
        timestamp: String?,
        setTimestamp: (timestamp: TemporalAccessor?) -> EmbedBuilder
    ) = timestamp?.let { setTimestamp(parseTimestamp(it)) }

    private fun setupFields(
        embedSetting: EmbedSetting,
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
    private fun buildComponents(actionRowSettings: List<MessageDataSerializer.ActionRowSetting>): List<LayoutComponent> {
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }

        val finalActionRows = mutableListOf<LayoutComponent>()

        actionRowSettings.forEach { actionRowSetting ->
            actionRowSetting.modelKey?.let {
                val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                when (model) {
                    is LayoutComponent -> finalActionRows.add(model)
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
            is ButtonsSetting -> buildButtons(actionRowSetting.buttons)
            is StringSelectMenuSetting -> buildStringSelectMenu(actionRowSetting)
            is EntitySelectMenuSetting -> buildEntitySelectMenu(actionRowSetting)
        }
    }

    private fun buildButtons(
        buttonsSetting: List<ButtonSetting>,
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

            finalButtons.add(
                ButtonImpl(
                    /* id = */ buttonSetting.uid?.let { componentIdManager.build(it) }
                        ?: buttonSetting.url?.let { strKey ->
                            parsePlaceholder(strKey)
                        } ?: throw NullPointerException("Either uid or url must be provided!"),
                    /* label = */ buttonSetting.label?.let { parsePlaceholder(it) },
                    /* style = */ when (buttonSetting.style) {
                        1 -> ButtonStyle.PRIMARY
                        2 -> ButtonStyle.SECONDARY
                        3 -> ButtonStyle.SUCCESS
                        4 -> ButtonStyle.DANGER
                        5 -> ButtonStyle.LINK
                        else -> throw IllegalArgumentException("Unknown style code: ${buttonSetting.style}")
                    },
                    /* disabled = */ buttonSetting.disabled,
                    /* emoji = */ buttonSetting.emoji?.let { emojiSetting ->
                        emojiSetting.modelKey?.let {
                            val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                            when (model) {
                                is Emoji -> model
                                else -> throw IllegalArgumentException("Unknown component model: $model")
                            }
                        } ?: Emoji.fromFormatted(parsePlaceholder(emojiSetting.formatted!!))
                    }
                )
            )
        }

        return ActionRow.of(finalButtons)
    }

    private fun buildStringSelectMenu(
        stringSelectMenuSetting: StringSelectMenuSetting,
    ): ActionRow {
        // Check again, IDE doesn't recognize the check in the caller function
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }

        val menu = StringSelectMenu
            .create(
                parsePlaceholder(componentIdManager.build(stringSelectMenuSetting.uid))
            ).apply {
                placeholder = stringSelectMenuSetting.placeholder?.let { parsePlaceholder(it) }
                minValues = stringSelectMenuSetting.min
                maxValues = stringSelectMenuSetting.max
                setupOptions(stringSelectMenuSetting.options, ::addOption, ::addOptions)
            }

        return ActionRow.of(menu.build())
    }

    private fun buildEntitySelectMenu(
        entitySelectMenuSetting: EntitySelectMenuSetting,
    ): ActionRow {
        // Check again, IDE doesn't recognize the check in the caller function
        requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }

        val menu = EntitySelectMenu
            .create(
                parsePlaceholder(componentIdManager.build(entitySelectMenuSetting.uid)),
                EntitySelectMenu.SelectTarget.valueOf(entitySelectMenuSetting.selectTargetType.uppercase())
            ).apply {
                placeholder = entitySelectMenuSetting.placeholder?.let { parsePlaceholder(it) }
                minValues = entitySelectMenuSetting.min
                maxValues = entitySelectMenuSetting.max
            }
        if (entitySelectMenuSetting.selectTargetType.uppercase() == "CHANNEL") {
            require(entitySelectMenuSetting.channelTypes.isNotEmpty()) {
                "'channel_types' cannot be empty when 'select_target_type' is set to 'CHANNEL'!"
            }
            menu.setChannelTypes(entitySelectMenuSetting.channelTypes.map { ChannelType.valueOf(it) })
        }

        return ActionRow.of(menu.build())
    }

    private fun setupOptions(
        options: List<OptionSetting>,
        addOption: (String, String, String?, Emoji?) -> Unit,
        addOptions: (SelectOption) -> Unit
    ) {

        options.forEach { option ->
            option.modelKey?.let {
                val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
                when (model) {
                    is SelectOption -> addOptions(model)
                    else -> throw IllegalArgumentException("Unknown option model: $model")
                }
                return@forEach
            }

            addOption(
                /* label = */ parsePlaceholder(option.label),
                /* value = */ parsePlaceholder(option.value),
                /* description = */ option.description?.let { parsePlaceholder(it) },
                /* emoji = */ option.emoji?.let { Emoji.fromUnicode(option.emoji.name) }
            )
        }
    }


    /**
     * 嘗試使用 substitutor 解析文字；若 substitutor == null，回傳原值。
     */
    private fun parsePlaceholder(text: String): String {
        return substitutor?.parse(text) ?: text
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
