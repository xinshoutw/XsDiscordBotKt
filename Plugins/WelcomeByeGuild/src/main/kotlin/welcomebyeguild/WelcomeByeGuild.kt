package tw.xinshou.discord.plugin.welcomebyeguild

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.util.ComponentIdManager
import tw.xinshou.discord.core.util.FieldType
import tw.xinshou.discord.plugin.welcomebyeguild.Event.componentPrefix
import tw.xinshou.discord.plugin.welcomebyeguild.Event.pluginDirectory
import tw.xinshou.discord.plugin.welcomebyeguild.message.MsgFileSerializer
import java.awt.Color
import java.io.File
import java.time.Instant

internal object WelcomeByeGuild {
    private object Actions {
        const val SELECT_CHANNEL = "select-channel"
        const val MODAL_WELCOME_TEXT = "modal-welcome-text"
        const val MODAL_BYE_TEXT = "modal-bye-text"
        const val MODAL_IMAGES = "modal-images"
        const val MODAL_COLORS = "modal-colors"
        const val PREVIEW_JOIN = "preview-join"
        const val PREVIEW_LEAVE = "preview-leave"
        const val CONFIRM_CREATE = "confirm-create"
    }

    private object Inputs {
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val THUMBNAIL = "thumbnail"
        const val IMAGE = "image"
        const val WELCOME_COLOR = "welcome-color"
        const val BYE_COLOR = "bye-color"
    }

    private object TextKeys {
        const val RESPONSE_GUILD_ONLY = "response.guildOnly"
        const val RESPONSE_SAVE_DONE = "response.saveDone"
        const val RESPONSE_INVALID_COLOR = "response.invalidColor"

        const val SETUP_TITLE = "setup.title"
        const val SETUP_DESCRIPTION = "setup.description"
        const val SETUP_SELECT_CHANNEL_PLACEHOLDER = "setup.selectChannelPlaceholder"
        const val SETUP_BTN_WELCOME_TEXT = "setup.buttons.welcomeText"
        const val SETUP_BTN_LEAVE_TEXT = "setup.buttons.leaveText"
        const val SETUP_BTN_IMAGES = "setup.buttons.images"
        const val SETUP_BTN_COLORS = "setup.buttons.colors"
        const val SETUP_BTN_PREVIEW_JOIN = "setup.buttons.previewJoin"
        const val SETUP_BTN_PREVIEW_LEAVE = "setup.buttons.previewLeave"
        const val SETUP_BTN_SAVE = "setup.buttons.save"

        const val SETUP_FIELD_OUTPUT_CHANNEL = "setup.fields.outputChannel"
        const val SETUP_FIELD_WELCOME_MESSAGE = "setup.fields.welcomeMessage"
        const val SETUP_FIELD_LEAVE_MESSAGE = "setup.fields.leaveMessage"
        const val SETUP_FIELD_THUMBNAIL = "setup.fields.thumbnail"
        const val SETUP_FIELD_IMAGE = "setup.fields.image"
        const val SETUP_FIELD_WELCOME_COLOR = "setup.fields.welcomeColor"
        const val SETUP_FIELD_LEAVE_COLOR = "setup.fields.leaveColor"
        const val SETUP_OUTPUT_CHANNEL_NOT_SET = "setup.outputChannelNotSet"
        const val SETUP_THUMBNAIL_NOT_SET = "setup.thumbnailNotSet"
        const val SETUP_IMAGE_NOT_SET = "setup.imageNotSet"

        const val MODAL_WELCOME_TITLE = "modal.welcomeTitle"
        const val MODAL_LEAVE_TITLE = "modal.leaveTitle"
        const val MODAL_IMAGES_TITLE = "modal.imagesTitle"
        const val MODAL_COLORS_TITLE = "modal.colorsTitle"
        const val MODAL_LABEL_TITLE = "modal.labels.title"
        const val MODAL_LABEL_DESCRIPTION = "modal.labels.description"
        const val MODAL_LABEL_THUMBNAIL_URL = "modal.labels.thumbnailUrl"
        const val MODAL_LABEL_IMAGE_URL = "modal.labels.imageUrl"
        const val MODAL_LABEL_WELCOME_COLOR = "modal.labels.welcomeColor"
        const val MODAL_LABEL_LEAVE_COLOR = "modal.labels.leaveColor"
        const val MODAL_PLACEHOLDER_THUMBNAIL_URL = "modal.placeholders.thumbnailUrl"
        const val MODAL_PLACEHOLDER_IMAGE_URL = "modal.placeholders.imageUrl"

        const val DEFAULT_WELCOME_TITLE = "defaults.welcomeTitle"
        const val DEFAULT_WELCOME_DESCRIPTION = "defaults.welcomeDescription"
        const val DEFAULT_LEAVE_TITLE = "defaults.leaveTitle"
        const val DEFAULT_LEAVE_DESCRIPTION = "defaults.leaveDescription"
    }

    private val colorRegex = Regex("^#?[0-9a-fA-F]{6}$")

    private val jsonAdapter: JsonAdapter<GuildSetting> = JsonFileManager.moshi.adapterReified<GuildSetting>()
    private val jsonGuildManager = JsonGuildFileManager(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = GuildSetting()
    )

    private val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
        )
    )

    private lateinit var textLocalizer: StringLocalizer<MsgFileSerializer>

    private val steps: MutableMap<Long, CreateStep> = hashMapOf()

    internal fun load() {
        textLocalizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = MsgFileSerializer::class,
            fileName = "message/text.yaml"
        )
    }

    internal fun reload() {
        steps.clear()
        load()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        jsonGuildManager.removeAndSave(event.guild.idLong)
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.reply(text(event.userLocale, TextKeys.RESPONSE_GUILD_ONLY)).setEphemeral(true).queue()
            return
        }

        val initialData = jsonGuildManager[guild.idLong].data.copy().also {
            ensureDefaults(it, event.userLocale)
        }

        fun setup(hook: InteractionHook) {
            val step = CreateStep(hook, guild.idLong, initialData)
            steps[event.user.idLong] = step
            renderSetup(step, event.userLocale).queue()
        }

        if (!event.isAcknowledged) {
            event.deferReply(true).queue { hook -> setup(hook) }
        } else {
            setup(event.hook)
        }
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
        if (event.guild?.idLong != step.guildId) return event.deferEdit().queue()

        val action = componentIdManager.parse(event.componentId)["action"] as String
        when (action) {
            Actions.MODAL_WELCOME_TEXT -> {
                event.replyModal(
                    buildTextModal(
                        modalAction = Actions.MODAL_WELCOME_TEXT,
                        title = text(event.userLocale, TextKeys.MODAL_WELCOME_TITLE),
                        defaultTitle = step.data.welcomeTitle,
                        defaultDescription = step.data.welcomeDescription,
                        locale = event.userLocale,
                    )
                ).queue()
            }

            Actions.MODAL_BYE_TEXT -> {
                event.replyModal(
                    buildTextModal(
                        modalAction = Actions.MODAL_BYE_TEXT,
                        title = text(event.userLocale, TextKeys.MODAL_LEAVE_TITLE),
                        defaultTitle = step.data.byeTitle,
                        defaultDescription = step.data.byeDescription,
                        locale = event.userLocale,
                    )
                ).queue()
            }

            Actions.MODAL_IMAGES -> {
                event.replyModal(buildImageModal(step, event.userLocale)).queue()
            }

            Actions.MODAL_COLORS -> {
                event.replyModal(buildColorModal(step, event.userLocale)).queue()
            }

            Actions.PREVIEW_JOIN -> {
                val guild = event.guild ?: return event.deferEdit().queue()
                event.replyEmbeds(createMemberEmbed(step.data, event.user, guild.name, guild.memberCount, true))
                    .setEphemeral(true)
                    .queue()
            }

            Actions.PREVIEW_LEAVE -> {
                val guild = event.guild ?: return event.deferEdit().queue()
                event.replyEmbeds(createMemberEmbed(step.data, event.user, guild.name, guild.memberCount, false))
                    .setEphemeral(true)
                    .queue()
            }

            Actions.CONFIRM_CREATE -> {
                val manager = jsonGuildManager[step.guildId]
                manager.data = step.data.copy()
                manager.save()
                steps.remove(event.user.idLong)

                event.deferEdit().flatMap {
                    step.hook.editOriginal(
                        MessageEditBuilder()
                            .setContent(text(event.userLocale, TextKeys.RESPONSE_SAVE_DONE))
                            .setEmbeds(buildSetupEmbed(step.data, event.userLocale))
                            .setComponents(emptyList())
                            .build()
                    )
                }.queue()
            }
        }
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
        if (event.guild?.idLong != step.guildId) return event.deferEdit().queue()

        val action = componentIdManager.parse(event.componentId)["action"] as String
        if (action != Actions.SELECT_CHANNEL) {
            event.deferEdit().queue()
            return
        }

        step.data.channelId = event.values.firstOrNull()?.idLong ?: 0L

        event.deferEdit().flatMap {
            renderSetup(step, event.userLocale)
        }.queue()
    }

    fun onModalInteraction(event: ModalInteractionEvent) {
        val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
        if (event.guild?.idLong != step.guildId) return event.deferEdit().queue()

        val action = componentIdManager.parse(event.modalId)["action"] as String
        when (action) {
            Actions.MODAL_WELCOME_TEXT -> {
                step.data.welcomeTitle = event.getValue(Inputs.TITLE)?.asString.orEmpty().ifBlank {
                    text(event.userLocale, TextKeys.DEFAULT_WELCOME_TITLE)
                }
                step.data.welcomeDescription = event.getValue(Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                    text(event.userLocale, TextKeys.DEFAULT_WELCOME_DESCRIPTION)
                }
            }

            Actions.MODAL_BYE_TEXT -> {
                step.data.byeTitle = event.getValue(Inputs.TITLE)?.asString.orEmpty().ifBlank {
                    text(event.userLocale, TextKeys.DEFAULT_LEAVE_TITLE)
                }
                step.data.byeDescription = event.getValue(Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                    text(event.userLocale, TextKeys.DEFAULT_LEAVE_DESCRIPTION)
                }
            }

            Actions.MODAL_IMAGES -> {
                step.data.thumbnailUrl = event.getValue(Inputs.THUMBNAIL)?.asString.orEmpty()
                step.data.imageUrl = event.getValue(Inputs.IMAGE)?.asString.orEmpty()
            }

            Actions.MODAL_COLORS -> {
                val welcomeColorRaw = event.getValue(Inputs.WELCOME_COLOR)?.asString.orEmpty()
                val byeColorRaw = event.getValue(Inputs.BYE_COLOR)?.asString.orEmpty()

                val welcomeColor = parseColor(welcomeColorRaw)
                val byeColor = parseColor(byeColorRaw)

                if (welcomeColor == null || byeColor == null) {
                    event.reply(text(event.userLocale, TextKeys.RESPONSE_INVALID_COLOR)).setEphemeral(true).queue()
                    return
                }

                step.data.welcomeColor = welcomeColor
                step.data.byeColor = byeColor
            }
        }

        event.deferEdit().flatMap {
            renderSetup(step, event.userLocale)
        }.queue()
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val guild = event.guild
        val data = jsonGuildManager.mapper[guild.idLong]?.data ?: return
        ensureDefaults(data, DiscordLocale.CHINESE_TAIWAN)

        val channel: TextChannel = guild.getTextChannelById(data.channelId) ?: return

        channel.sendMessageEmbeds(
            createMemberEmbed(
                data = data,
                user = event.user,
                guildName = guild.name,
                memberCount = guild.memberCount,
                isJoin = true,
            )
        ).queue()
    }

    fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val guild = event.guild
        val data = jsonGuildManager.mapper[guild.idLong]?.data ?: return
        ensureDefaults(data, DiscordLocale.CHINESE_TAIWAN)

        val channel: TextChannel = guild.getTextChannelById(data.channelId) ?: return

        channel.sendMessageEmbeds(
            createMemberEmbed(
                data = data,
                user = event.user,
                guildName = guild.name,
                memberCount = guild.memberCount,
                isJoin = false,
            )
        ).queue()
    }

    private fun renderSetup(step: CreateStep, locale: DiscordLocale): WebhookMessageEditAction<Message?> {
        val channelSelector = EntitySelectMenu.create(
            componentIdManager.build(mapOf("action" to Actions.SELECT_CHANNEL)),
            EntitySelectMenu.SelectTarget.CHANNEL
        )
            .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
            .setRequiredRange(1, 1)
            .setPlaceholder(text(locale, TextKeys.SETUP_SELECT_CHANNEL_PLACEHOLDER))
            .build()

        val row2 = ActionRow.of(
            Button.primary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_WELCOME_TEXT)),
                text(locale, TextKeys.SETUP_BTN_WELCOME_TEXT)
            ),
            Button.primary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_BYE_TEXT)),
                text(locale, TextKeys.SETUP_BTN_LEAVE_TEXT)
            ),
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_IMAGES)),
                text(locale, TextKeys.SETUP_BTN_IMAGES)
            ),
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_COLORS)),
                text(locale, TextKeys.SETUP_BTN_COLORS)
            )
        )

        val row3 = ActionRow.of(
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.PREVIEW_JOIN)),
                text(locale, TextKeys.SETUP_BTN_PREVIEW_JOIN)
            ),
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.PREVIEW_LEAVE)),
                text(locale, TextKeys.SETUP_BTN_PREVIEW_LEAVE)
            ),
            Button.success(
                componentIdManager.build(mapOf("action" to Actions.CONFIRM_CREATE)),
                text(locale, TextKeys.SETUP_BTN_SAVE)
            ).withDisabled(step.data.channelId == 0L)
        )

        return step.hook.editOriginal(
            MessageEditBuilder()
                .setContent(null)
                .setEmbeds(buildSetupEmbed(step.data, locale))
                .setComponents(ActionRow.of(channelSelector), row2, row3)
                .build()
        )
    }

    private fun buildSetupEmbed(data: GuildSetting, locale: DiscordLocale) = EmbedBuilder().apply {
        setTitle(text(locale, TextKeys.SETUP_TITLE))
        setDescription(text(locale, TextKeys.SETUP_DESCRIPTION))

        addField(
            text(locale, TextKeys.SETUP_FIELD_OUTPUT_CHANNEL),
            if (data.channelId == 0L) text(locale, TextKeys.SETUP_OUTPUT_CHANNEL_NOT_SET) else "<#${data.channelId}>",
            false
        )

        addField(
            text(locale, TextKeys.SETUP_FIELD_WELCOME_MESSAGE),
            truncateLine("${data.welcomeTitle}\n${data.welcomeDescription}"),
            false
        )

        addField(
            text(locale, TextKeys.SETUP_FIELD_LEAVE_MESSAGE),
            truncateLine("${data.byeTitle}\n${data.byeDescription}"),
            false
        )

        addField(
            text(locale, TextKeys.SETUP_FIELD_THUMBNAIL),
            if (data.thumbnailUrl.isBlank()) text(locale, TextKeys.SETUP_THUMBNAIL_NOT_SET) else data.thumbnailUrl,
            false
        )

        addField(
            text(locale, TextKeys.SETUP_FIELD_IMAGE),
            if (data.imageUrl.isBlank()) text(locale, TextKeys.SETUP_IMAGE_NOT_SET) else data.imageUrl,
            false
        )

        addField(
            text(locale, TextKeys.SETUP_FIELD_WELCOME_COLOR),
            String.format("#%06X", data.welcomeColor and 0xFFFFFF),
            true
        )

        addField(
            text(locale, TextKeys.SETUP_FIELD_LEAVE_COLOR),
            String.format("#%06X", data.byeColor and 0xFFFFFF),
            true
        )

        setColor(Color(0x5865F2))
        setTimestamp(Instant.now())
    }.build()

    private fun buildTextModal(
        modalAction: String,
        title: String,
        defaultTitle: String,
        defaultDescription: String,
        locale: DiscordLocale,
    ): Modal {
        val titleInput = TextInput.create(Inputs.TITLE, TextInputStyle.SHORT)
            .setRequired(true)
            .setMaxLength(256)
            .setValue(defaultTitle)
            .build()

        val descriptionInput = TextInput.create(Inputs.DESCRIPTION, TextInputStyle.PARAGRAPH)
            .setRequired(true)
            .setMaxLength(1500)
            .setValue(defaultDescription)
            .build()

        return Modal.create(componentIdManager.build(mapOf("action" to modalAction)), title)
            .addComponents(
                Label.of(text(locale, TextKeys.MODAL_LABEL_TITLE), titleInput),
                Label.of(text(locale, TextKeys.MODAL_LABEL_DESCRIPTION), descriptionInput),
            )
            .build()
    }

    private fun buildImageModal(step: CreateStep, locale: DiscordLocale): Modal {
        val thumbnailInput = TextInput.create(Inputs.THUMBNAIL, TextInputStyle.SHORT)
            .setRequired(false)
            .setMaxLength(1000)
            .setPlaceholder(text(locale, TextKeys.MODAL_PLACEHOLDER_THUMBNAIL_URL))
            .setValue(step.data.thumbnailUrl.ifBlank { null })
            .build()

        val imageInput = TextInput.create(Inputs.IMAGE, TextInputStyle.SHORT)
            .setRequired(false)
            .setMaxLength(1000)
            .setPlaceholder(text(locale, TextKeys.MODAL_PLACEHOLDER_IMAGE_URL))
            .setValue(step.data.imageUrl.ifBlank { null })
            .build()

        return Modal.create(
            componentIdManager.build(mapOf("action" to Actions.MODAL_IMAGES)),
            text(locale, TextKeys.MODAL_IMAGES_TITLE)
        )
            .addComponents(
                Label.of(text(locale, TextKeys.MODAL_LABEL_THUMBNAIL_URL), thumbnailInput),
                Label.of(text(locale, TextKeys.MODAL_LABEL_IMAGE_URL), imageInput),
            )
            .build()
    }

    private fun buildColorModal(step: CreateStep, locale: DiscordLocale): Modal {
        val welcomeColor = TextInput.create(Inputs.WELCOME_COLOR, TextInputStyle.SHORT)
            .setRequired(true)
            .setMinLength(7)
            .setMaxLength(7)
            .setValue(String.format("#%06X", step.data.welcomeColor and 0xFFFFFF))
            .build()

        val byeColor = TextInput.create(Inputs.BYE_COLOR, TextInputStyle.SHORT)
            .setRequired(true)
            .setMinLength(7)
            .setMaxLength(7)
            .setValue(String.format("#%06X", step.data.byeColor and 0xFFFFFF))
            .build()

        return Modal.create(
            componentIdManager.build(mapOf("action" to Actions.MODAL_COLORS)),
            text(locale, TextKeys.MODAL_COLORS_TITLE)
        )
            .addComponents(
                Label.of(text(locale, TextKeys.MODAL_LABEL_WELCOME_COLOR), welcomeColor),
                Label.of(text(locale, TextKeys.MODAL_LABEL_LEAVE_COLOR), byeColor),
            )
            .build()
    }

    private fun createMemberEmbed(
        data: GuildSetting,
        user: User,
        guildName: String,
        memberCount: Int,
        isJoin: Boolean,
    ) = EmbedBuilder().apply {
        val titleTemplate = if (isJoin) data.welcomeTitle else data.byeTitle
        val descriptionTemplate = if (isJoin) data.welcomeDescription else data.byeDescription

        setTitle(parseTemplate(titleTemplate, user, guildName, memberCount))
        setDescription(parseTemplate(descriptionTemplate, user, guildName, memberCount))

        val thumbnail = data.thumbnailUrl.ifBlank { user.effectiveAvatarUrl ?: "" }
        if (thumbnail.isNotBlank()) {
            setThumbnail(thumbnail)
        }

        if (data.imageUrl.isNotBlank()) {
            setImage(data.imageUrl)
        }

        setColor(Color(if (isJoin) data.welcomeColor else data.byeColor))
        setTimestamp(Instant.now())
    }.build()

    private fun parseTemplate(template: String, user: User, guildName: String, memberCount: Int): String {
        return template
            .replace("{userMention}", user.asMention)
            .replace("{userName}", user.name)
            .replace("{guildName}", guildName)
            .replace("{memberCount}", memberCount.toString())
    }

    private fun parseColor(input: String): Int? {
        if (!colorRegex.matches(input)) return null
        return input.removePrefix("#").toInt(16)
    }

    private fun truncateLine(value: String, maxLength: Int = 600): String {
        if (value.length <= maxLength) return value
        return value.take(maxLength - 3) + "..."
    }

    private fun ensureDefaults(data: GuildSetting, locale: DiscordLocale) {
        if (data.welcomeTitle.isBlank()) {
            data.welcomeTitle = text(locale, TextKeys.DEFAULT_WELCOME_TITLE)
        }

        if (data.welcomeDescription.isBlank()) {
            data.welcomeDescription = text(locale, TextKeys.DEFAULT_WELCOME_DESCRIPTION)
        }

        if (data.byeTitle.isBlank()) {
            data.byeTitle = text(locale, TextKeys.DEFAULT_LEAVE_TITLE)
        }

        if (data.byeDescription.isBlank()) {
            data.byeDescription = text(locale, TextKeys.DEFAULT_LEAVE_DESCRIPTION)
        }
    }

    private fun text(locale: DiscordLocale, key: String): String = textLocalizer.get(key, locale)

    private data class CreateStep(
        val hook: InteractionHook,
        val guildId: Long,
        val data: GuildSetting,
    )
}

internal data class GuildSetting(
    var channelId: Long = 0L,
    var welcomeTitle: String = "",
    var welcomeDescription: String = "",
    var byeTitle: String = "",
    var byeDescription: String = "",
    var thumbnailUrl: String = "",
    var imageUrl: String = "",
    var welcomeColor: Int = 0x57F287,
    var byeColor: Int = 0xED4245,
)
