package tw.xinshou.discord.plugin.welcomebyeguild

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
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
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import tw.xinshou.discord.core.builtin.messagecreator.modal.ModalCreator
import tw.xinshou.discord.core.builtin.messagecreator.v2.MessageCreator
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.core.util.ComponentIdManager
import tw.xinshou.discord.core.util.FieldType
import tw.xinshou.discord.plugin.welcomebyeguild.Event.componentPrefix
import tw.xinshou.discord.plugin.welcomebyeguild.Event.pluginDirectory
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

    private object MessageKeys {
        const val GUILD_ONLY = "guild-only"
        const val INVALID_COLOR = "invalid-color"
        const val CHANNEL_NOT_SET = "channel-not-set"

        const val SETUP_PANEL = "setup-panel"
        const val SETUP_SAVED = "setup-saved"

        const val DEFAULT_JOIN = "default-join"
        const val DEFAULT_LEAVE = "default-leave"
    }

    private object ModalKeys {
        const val WELCOME_TEXT = "welcome-text"
        const val LEAVE_TEXT = "leave-text"
        const val IMAGES = "images"
        const val COLORS = "colors"
    }

    private object Models {
        const val PREVIEW_EMBED = "wbg@preview-embed"
    }

    private val colorRegex = Regex("^#?[0-9a-fA-F]{6}$")
    private val defaultLocale: DiscordLocale = DiscordLocale.CHINESE_TAIWAN

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

    private lateinit var messageCreator: MessageCreator
    private lateinit var modalCreator: ModalCreator

    private val steps: MutableMap<Long, CreateStep> = hashMapOf()

    internal fun load() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = defaultLocale,
            componentIdManager = componentIdManager,
        )

        modalCreator = ModalCreator(
            langDirFile = File(pluginDirectory, "lang"),
            componentIdManager = componentIdManager,
            defaultLocale = defaultLocale
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
            event.reply(createMessage(MessageKeys.GUILD_ONLY, event.userLocale)).setEphemeral(true).queue()
            return
        }

        val initialData = jsonGuildManager[guild.idLong].data.copy().also {
            ensureDefaults(it, event.userLocale)
        }

        fun setup(hook: InteractionHook) {
            val step = CreateStep(
                hook = hook,
                guildId = guild.idLong,
                previewUser = event.user,
                previewGuildName = guild.name,
                previewMemberCount = guild.memberCount,
                data = initialData
            )
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
                val substitutor = Placeholder.get(event).putAll(
                    "wbg@title" to step.data.welcomeTitle,
                    "wbg@description" to step.data.welcomeDescription,
                )

                event.replyModal(
                    modalCreator.getModalBuilder(
                        ModalKeys.WELCOME_TEXT,
                        event.userLocale,
                        substitutor = substitutor,
                    ).build()
                ).queue()
            }

            Actions.MODAL_BYE_TEXT -> {
                val substitutor = Placeholder.get(event).putAll(
                    "wbg@title" to step.data.byeTitle,
                    "wbg@description" to step.data.byeDescription,
                )

                event.replyModal(
                    modalCreator.getModalBuilder(
                        ModalKeys.LEAVE_TEXT,
                        event.userLocale,
                        substitutor = substitutor,
                    ).build()
                ).queue()
            }

            Actions.MODAL_IMAGES -> {
                val substitutor = Placeholder.get(event).putAll(
                    "wbg@thumbnail" to step.data.thumbnailUrl,
                    "wbg@image" to step.data.imageUrl,
                )

                event.replyModal(
                    modalCreator.getModalBuilder(
                        ModalKeys.IMAGES,
                        event.userLocale,
                        substitutor = substitutor,
                    ).build()
                ).queue()
            }

            Actions.MODAL_COLORS -> {
                val substitutor = Placeholder.get(event).putAll(
                    "wbg@welcome-color" to String.format("#%06X", step.data.welcomeColor and 0xFFFFFF),
                    "wbg@bye-color" to String.format("#%06X", step.data.byeColor and 0xFFFFFF),
                )

                event.replyModal(
                    modalCreator.getModalBuilder(
                        ModalKeys.COLORS,
                        event.userLocale,
                        substitutor = substitutor,
                    ).build()
                ).queue()
            }

            Actions.PREVIEW_JOIN -> {
                step.previewType = PreviewType.JOIN
                event.deferEdit().flatMap {
                    renderSetup(step, event.userLocale)
                }.queue()
            }

            Actions.PREVIEW_LEAVE -> {
                step.previewType = PreviewType.LEAVE
                event.deferEdit().flatMap {
                    renderSetup(step, event.userLocale)
                }.queue()
            }

            Actions.CONFIRM_CREATE -> {
                if (step.data.channelId == 0L) {
                    event.reply(createMessage(MessageKeys.CHANNEL_NOT_SET, event.userLocale)).setEphemeral(true).queue()
                    return
                }

                val manager = jsonGuildManager[step.guildId]
                manager.data = step.data.copy()
                manager.save()
                steps.remove(event.user.idLong)

                event.deferEdit().flatMap {
                    step.hook.editOriginal(
                        messageCreator.getEditBuilder(
                            MessageKeys.SETUP_SAVED,
                            event.userLocale,
                            modelMapper = mapOf(
                                Models.PREVIEW_EMBED to createPreviewEmbed(step),
                            )
                        ).build()
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
                    defaultTemplate(MessageKeys.DEFAULT_JOIN, event.userLocale).title
                }
                step.data.welcomeDescription = event.getValue(Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                    defaultTemplate(MessageKeys.DEFAULT_JOIN, event.userLocale).description
                }
            }

            Actions.MODAL_BYE_TEXT -> {
                step.data.byeTitle = event.getValue(Inputs.TITLE)?.asString.orEmpty().ifBlank {
                    defaultTemplate(MessageKeys.DEFAULT_LEAVE, event.userLocale).title
                }
                step.data.byeDescription = event.getValue(Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                    defaultTemplate(MessageKeys.DEFAULT_LEAVE, event.userLocale).description
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
                    event.reply(createMessage(MessageKeys.INVALID_COLOR, event.userLocale)).setEphemeral(true).queue()
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
        ensureDefaults(data, defaultLocale)

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
        ensureDefaults(data, defaultLocale)

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
        return step.hook.editOriginal(
            messageCreator.getEditBuilder(
                MessageKeys.SETUP_PANEL,
                locale,
                modelMapper = mapOf(
                    Models.PREVIEW_EMBED to createPreviewEmbed(step),
                )
            ).build()
        )
    }

    private fun createPreviewEmbed(step: CreateStep): MessageEmbed = createMemberEmbed(
        data = step.data,
        user = step.previewUser,
        guildName = step.previewGuildName,
        memberCount = step.previewMemberCount,
        isJoin = step.previewType == PreviewType.JOIN,
    )

    private fun createMemberEmbed(
        data: GuildSetting,
        user: User,
        guildName: String,
        memberCount: Int,
        isJoin: Boolean,
    ) = net.dv8tion.jda.api.EmbedBuilder().apply {
        val titleTemplate = if (isJoin) data.welcomeTitle else data.byeTitle
        val descriptionTemplate = if (isJoin) data.welcomeDescription else data.byeDescription

        setTitle(parseTemplate(titleTemplate, user, guildName, memberCount))
        setDescription(parseTemplate(descriptionTemplate, user, guildName, memberCount))

        val thumbnail = data.thumbnailUrl.ifBlank { user.effectiveAvatarUrl }
        if (thumbnail.isNotBlank()) {
            setThumbnail(thumbnail)
        }

        if (data.imageUrl.isNotBlank()) {
            setImage(data.imageUrl)
        }

        setColor(java.awt.Color(if (isJoin) data.welcomeColor else data.byeColor))
        setTimestamp(Instant.now())
    }.build()

    private fun parseTemplate(template: String, user: User, guildName: String, memberCount: Int): String {
        return Placeholder.get(user)
            .putAll(
                "wbg@guild_name" to guildName,
                "wbg@member_count" to memberCount.toString(),
            )
            .parse(template)
    }

    private fun parseColor(input: String): Int? {
        if (!colorRegex.matches(input)) return null
        return input.removePrefix("#").toInt(16)
    }

    private fun ensureDefaults(data: GuildSetting, locale: DiscordLocale) {
        val defaultJoin = defaultTemplate(MessageKeys.DEFAULT_JOIN, locale)
        val defaultLeave = defaultTemplate(MessageKeys.DEFAULT_LEAVE, locale)

        if (data.welcomeTitle.isBlank()) {
            data.welcomeTitle = defaultJoin.title
        }

        if (data.welcomeDescription.isBlank()) {
            data.welcomeDescription = defaultJoin.description
        }

        if (data.byeTitle.isBlank()) {
            data.byeTitle = defaultLeave.title
        }

        if (data.byeDescription.isBlank()) {
            data.byeDescription = defaultLeave.description
        }
    }

    private fun defaultTemplate(key: String, locale: DiscordLocale): Template = messageCreator
        .getCreateBuilder(key, locale)
        .build()
        .embeds
        .first()
        .let { embed ->
            Template(
                title = requireNotNull(embed.title) { "title missing in $key" },
                description = requireNotNull(embed.description) { "description missing in $key" },
            )
        }

    private fun createMessage(
        key: String,
        locale: DiscordLocale,
        replaceMap: Map<String, String> = emptyMap(),
    ) = messageCreator.getCreateBuilder(key, locale, replaceMap).build()

    private data class CreateStep(
        val hook: InteractionHook,
        val guildId: Long,
        val previewUser: User,
        val previewGuildName: String,
        val previewMemberCount: Int,
        var previewType: PreviewType = PreviewType.JOIN,
        val data: GuildSetting,
    )

    private data class Template(
        val title: String,
        val description: String,
    )

    private enum class PreviewType {
        JOIN,
        LEAVE,
    }
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
