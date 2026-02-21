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
import tw.xinshou.discord.core.util.ComponentIdManager
import tw.xinshou.discord.core.util.FieldType
import tw.xinshou.discord.plugin.welcomebyeguild.Event.componentPrefix
import tw.xinshou.discord.plugin.welcomebyeguild.Event.pluginDirectory
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

    private val steps: MutableMap<Long, CreateStep> = hashMapOf()

    internal fun reload() {
        steps.clear()
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        jsonGuildManager.removeAndSave(event.guild.idLong)
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.reply("This command can only be used in a guild.").setEphemeral(true).queue()
            return
        }

        val initialData = jsonGuildManager[guild.idLong].data.copy()

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
                        title = if (isZhLocale(event.userLocale)) "設定歡迎訊息" else "Set welcome message",
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
                        title = if (isZhLocale(event.userLocale)) "設定離開訊息" else "Set leave message",
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

                val doneMessage = if (isZhLocale(event.userLocale)) {
                    "WelcomeByeGuild 設定已儲存，已開始監聽成員加入/離開事件。"
                } else {
                    "WelcomeByeGuild settings saved. Join/leave notifications are now active."
                }

                event.deferEdit().flatMap {
                    step.hook.editOriginal(
                        MessageEditBuilder()
                            .setContent(doneMessage)
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
                    defaultWelcomeTitle
                }
                step.data.welcomeDescription = event.getValue(Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                    defaultWelcomeDescription
                }
            }

            Actions.MODAL_BYE_TEXT -> {
                step.data.byeTitle = event.getValue(Inputs.TITLE)?.asString.orEmpty().ifBlank {
                    defaultByeTitle
                }
                step.data.byeDescription = event.getValue(Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                    defaultByeDescription
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
                    val errorMessage = if (isZhLocale(event.userLocale)) {
                        "顏色格式錯誤，請輸入 #RRGGBB。"
                    } else {
                        "Invalid color format. Please use #RRGGBB."
                    }
                    event.reply(errorMessage).setEphemeral(true).queue()
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
        val isZh = isZhLocale(locale)

        val channelSelector = EntitySelectMenu.create(
            componentIdManager.build(mapOf("action" to Actions.SELECT_CHANNEL)),
            EntitySelectMenu.SelectTarget.CHANNEL
        )
            .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
            .setRequiredRange(1, 1)
            .setPlaceholder(if (isZh) "選擇訊息輸出頻道" else "Select output channel")
            .build()

        val row2 = ActionRow.of(
            Button.primary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_WELCOME_TEXT)),
                if (isZh) "設定歡迎訊息" else "Welcome text"
            ),
            Button.primary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_BYE_TEXT)),
                if (isZh) "設定離開訊息" else "Leave text"
            ),
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_IMAGES)),
                if (isZh) "設定圖片" else "Images"
            ),
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.MODAL_COLORS)),
                if (isZh) "設定顏色" else "Colors"
            )
        )

        val row3 = ActionRow.of(
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.PREVIEW_JOIN)),
                if (isZh) "預覽加入" else "Preview join"
            ),
            Button.secondary(
                componentIdManager.build(mapOf("action" to Actions.PREVIEW_LEAVE)),
                if (isZh) "預覽離開" else "Preview leave"
            ),
            Button.success(
                componentIdManager.build(mapOf("action" to Actions.CONFIRM_CREATE)),
                if (isZh) "儲存設定" else "Save"
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
        val isZh = isZhLocale(locale)
        setTitle(if (isZh) "WelcomeByeGuild 建立精靈" else "WelcomeByeGuild Setup")
        setDescription(
            if (isZh) {
                "請設定加入/離開通知內容，完成後按下 `儲存設定`。"
            } else {
                "Configure join/leave notification messages, then click `Save`."
            }
        )

        addField(
            if (isZh) "輸出頻道" else "Output channel",
            if (data.channelId == 0L) {
                if (isZh) "尚未設定" else "Not set"
            } else {
                "<#${data.channelId}>"
            },
            false
        )

        addField(
            if (isZh) "歡迎訊息" else "Welcome message",
            truncateLine("${data.welcomeTitle}\n${data.welcomeDescription}"),
            false
        )

        addField(
            if (isZh) "離開訊息" else "Leave message",
            truncateLine("${data.byeTitle}\n${data.byeDescription}"),
            false
        )

        addField(
            if (isZh) "縮圖 (thumbnail)" else "Thumbnail",
            if (data.thumbnailUrl.isBlank()) {
                if (isZh) "未設定 (將使用使用者頭像)" else "Not set (uses user avatar)"
            } else {
                data.thumbnailUrl
            },
            false
        )

        addField(
            if (isZh) "主圖 (photo/image)" else "Image",
            if (data.imageUrl.isBlank()) {
                if (isZh) "未設定" else "Not set"
            } else {
                data.imageUrl
            },
            false
        )

        addField(
            if (isZh) "歡迎顏色" else "Welcome color",
            String.format("#%06X", data.welcomeColor and 0xFFFFFF),
            true
        )

        addField(
            if (isZh) "離開顏色" else "Leave color",
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
        val isZh = isZhLocale(locale)

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
                Label.of(if (isZh) "標題" else "Title", titleInput),
                Label.of(if (isZh) "描述" else "Description", descriptionInput),
            )
            .build()
    }

    private fun buildImageModal(step: CreateStep, locale: DiscordLocale): Modal {
        val isZh = isZhLocale(locale)

        val thumbnailInput = TextInput.create(Inputs.THUMBNAIL, TextInputStyle.SHORT)
            .setRequired(false)
            .setMaxLength(1000)
            .setPlaceholder(if (isZh) "留空 = 使用使用者頭像" else "Empty = use user avatar")
            .setValue(step.data.thumbnailUrl.ifBlank { null })
            .build()

        val imageInput = TextInput.create(Inputs.IMAGE, TextInputStyle.SHORT)
            .setRequired(false)
            .setMaxLength(1000)
            .setPlaceholder(if (isZh) "留空 = 不使用主圖" else "Empty = no image")
            .setValue(step.data.imageUrl.ifBlank { null })
            .build()

        return Modal.create(
            componentIdManager.build(mapOf("action" to Actions.MODAL_IMAGES)),
            if (isZh) "設定圖片" else "Set images"
        )
            .addComponents(
                Label.of(if (isZh) "縮圖 URL" else "Thumbnail URL", thumbnailInput),
                Label.of(if (isZh) "主圖 URL" else "Image URL", imageInput),
            )
            .build()
    }

    private fun buildColorModal(step: CreateStep, locale: DiscordLocale): Modal {
        val isZh = isZhLocale(locale)

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
            if (isZh) "設定顏色" else "Set colors"
        )
            .addComponents(
                Label.of(if (isZh) "歡迎顏色 (#RRGGBB)" else "Welcome color (#RRGGBB)", welcomeColor),
                Label.of(if (isZh) "離開顏色 (#RRGGBB)" else "Leave color (#RRGGBB)", byeColor),
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

    private fun isZhLocale(locale: DiscordLocale): Boolean =
        locale == DiscordLocale.CHINESE_TAIWAN || locale == DiscordLocale.CHINESE_CHINA

    private data class CreateStep(
        val hook: InteractionHook,
        val guildId: Long,
        val data: GuildSetting,
    )

    private const val defaultWelcomeTitle = "🎉 歡迎 {userMention}"
    private const val defaultWelcomeDescription = "歡迎來到 **{guildName}**！你是第 **{memberCount}** 位成員。"
    private const val defaultByeTitle = "😢 {userName} 離開了"
    private const val defaultByeDescription = "祝你一切順利，期待再次見面。"
}

internal data class GuildSetting(
    var channelId: Long = 0L,
    var welcomeTitle: String = "🎉 歡迎 {userMention}",
    var welcomeDescription: String = "歡迎來到 **{guildName}**！你是第 **{memberCount}** 位成員。",
    var byeTitle: String = "😢 {userName} 離開了",
    var byeDescription: String = "祝你一切順利，期待再次見面。",
    var thumbnailUrl: String = "",
    var imageUrl: String = "",
    var welcomeColor: Int = 0x57F287,
    var byeColor: Int = 0xED4245,
)
