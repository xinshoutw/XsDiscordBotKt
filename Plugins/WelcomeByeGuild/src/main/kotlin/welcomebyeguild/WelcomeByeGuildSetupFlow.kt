package tw.xinshou.discord.plugin.welcomebyeguild

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import tw.xinshou.discord.core.builtin.placeholder.Placeholder

internal fun WelcomeByeGuild.handleSlashCommandInteraction(event: SlashCommandInteractionEvent) {
    val guild = event.guild ?: run {
        event.reply(createMessage(WelcomeByeGuild.MessageKeys.GUILD_ONLY, event.userLocale)).setEphemeral(true).queue()
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

internal fun WelcomeByeGuild.handleButtonInteraction(event: ButtonInteractionEvent) {
    val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
    if (event.guild?.idLong != step.guildId) return event.deferEdit().queue()

    val action = componentIdManager.parse(event.componentId)["action"] as String
    when (action) {
        WelcomeByeGuild.Actions.MODAL_WELCOME_TEXT -> {
            val substitutor = Placeholder.get(event).putAll(
                "wbg@title" to step.data.welcomeTitle,
                "wbg@description" to step.data.welcomeDescription,
            )

            event.replyModal(
                modalCreator.getModalBuilder(
                    WelcomeByeGuild.ModalKeys.WELCOME_TEXT,
                    event.userLocale,
                    substitutor = substitutor,
                ).build()
            ).queue()
        }

        WelcomeByeGuild.Actions.MODAL_BYE_TEXT -> {
            val substitutor = Placeholder.get(event).putAll(
                "wbg@title" to step.data.byeTitle,
                "wbg@description" to step.data.byeDescription,
            )

            event.replyModal(
                modalCreator.getModalBuilder(
                    WelcomeByeGuild.ModalKeys.LEAVE_TEXT,
                    event.userLocale,
                    substitutor = substitutor,
                ).build()
            ).queue()
        }

        WelcomeByeGuild.Actions.MODAL_IMAGES -> {
            val substitutor = Placeholder.get(event).putAll(
                "wbg@thumbnail" to step.data.thumbnailUrl,
                "wbg@image" to step.data.imageUrl,
            )

            event.replyModal(
                modalCreator.getModalBuilder(
                    WelcomeByeGuild.ModalKeys.IMAGES,
                    event.userLocale,
                    substitutor = substitutor,
                ).build()
            ).queue()
        }

        WelcomeByeGuild.Actions.MODAL_COLORS -> {
            val substitutor = Placeholder.get(event).putAll(
                "wbg@welcome-color" to String.format("#%06X", step.data.welcomeColor and 0xFFFFFF),
                "wbg@bye-color" to String.format("#%06X", step.data.byeColor and 0xFFFFFF),
            )

            event.replyModal(
                modalCreator.getModalBuilder(
                    WelcomeByeGuild.ModalKeys.COLORS,
                    event.userLocale,
                    substitutor = substitutor,
                ).build()
            ).queue()
        }

        WelcomeByeGuild.Actions.PREVIEW_JOIN -> {
            step.previewType = PreviewType.JOIN
            event.deferEdit().flatMap {
                renderSetup(step, event.userLocale)
            }.queue()
        }

        WelcomeByeGuild.Actions.PREVIEW_LEAVE -> {
            step.previewType = PreviewType.LEAVE
            event.deferEdit().flatMap {
                renderSetup(step, event.userLocale)
            }.queue()
        }

        WelcomeByeGuild.Actions.CONFIRM_CREATE -> {
            if (step.data.channelId == 0L) {
                event.reply(createMessage(WelcomeByeGuild.MessageKeys.CHANNEL_NOT_SET, event.userLocale)).setEphemeral(true)
                    .queue()
                return
            }

            val manager = jsonGuildManager[step.guildId]
            manager.data = step.data.copy()
            manager.save()
            steps.remove(event.user.idLong)

            event.deferEdit().flatMap {
                step.hook.editOriginal(
                    messageCreator.getEditBuilder(
                        WelcomeByeGuild.MessageKeys.SETUP_SAVED,
                        event.userLocale,
                        modelMapper = mapOf(
                            WelcomeByeGuild.Models.PREVIEW_EMBED to createPreviewEmbed(step),
                        )
                    ).build()
                )
            }.queue()
        }
    }
}

internal fun WelcomeByeGuild.handleEntitySelectInteraction(event: EntitySelectInteractionEvent) {
    val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
    if (event.guild?.idLong != step.guildId) return event.deferEdit().queue()

    val action = componentIdManager.parse(event.componentId)["action"] as String
    if (action != WelcomeByeGuild.Actions.SELECT_CHANNEL) {
        event.deferEdit().queue()
        return
    }

    step.data.channelId = event.values.firstOrNull()?.idLong ?: 0L

    event.deferEdit().flatMap {
        renderSetup(step, event.userLocale)
    }.queue()
}

internal fun WelcomeByeGuild.handleModalInteraction(event: ModalInteractionEvent) {
    val step = steps[event.user.idLong] ?: return event.deferEdit().queue()
    if (event.guild?.idLong != step.guildId) return event.deferEdit().queue()

    val action = componentIdManager.parse(event.modalId)["action"] as String
    when (action) {
        WelcomeByeGuild.Actions.MODAL_WELCOME_TEXT -> {
            step.data.welcomeTitle = event.getValue(WelcomeByeGuild.Inputs.TITLE)?.asString.orEmpty().ifBlank {
                defaultTemplate(WelcomeByeGuild.MessageKeys.DEFAULT_JOIN, event.userLocale).title
            }
            step.data.welcomeDescription =
                event.getValue(WelcomeByeGuild.Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                    defaultTemplate(WelcomeByeGuild.MessageKeys.DEFAULT_JOIN, event.userLocale).description
                }
        }

        WelcomeByeGuild.Actions.MODAL_BYE_TEXT -> {
            step.data.byeTitle = event.getValue(WelcomeByeGuild.Inputs.TITLE)?.asString.orEmpty().ifBlank {
                defaultTemplate(WelcomeByeGuild.MessageKeys.DEFAULT_LEAVE, event.userLocale).title
            }
            step.data.byeDescription = event.getValue(WelcomeByeGuild.Inputs.DESCRIPTION)?.asString.orEmpty().ifBlank {
                defaultTemplate(WelcomeByeGuild.MessageKeys.DEFAULT_LEAVE, event.userLocale).description
            }
        }

        WelcomeByeGuild.Actions.MODAL_IMAGES -> {
            step.data.thumbnailUrl = event.getValue(WelcomeByeGuild.Inputs.THUMBNAIL)?.asString.orEmpty()
            step.data.imageUrl = event.getValue(WelcomeByeGuild.Inputs.IMAGE)?.asString.orEmpty()
        }

        WelcomeByeGuild.Actions.MODAL_COLORS -> {
            val welcomeColorRaw = event.getValue(WelcomeByeGuild.Inputs.WELCOME_COLOR)?.asString.orEmpty()
            val byeColorRaw = event.getValue(WelcomeByeGuild.Inputs.BYE_COLOR)?.asString.orEmpty()

            val welcomeColor = parseColor(welcomeColorRaw)
            val byeColor = parseColor(byeColorRaw)

            if (welcomeColor == null || byeColor == null) {
                event.reply(createMessage(WelcomeByeGuild.MessageKeys.INVALID_COLOR, event.userLocale)).setEphemeral(true)
                    .queue()
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

private fun WelcomeByeGuild.renderSetup(step: CreateStep, locale: DiscordLocale): WebhookMessageEditAction<Message?> {
    return step.hook.editOriginal(
        messageCreator.getEditBuilder(
            WelcomeByeGuild.MessageKeys.SETUP_PANEL,
            locale,
            modelMapper = mapOf(
                WelcomeByeGuild.Models.PREVIEW_EMBED to createPreviewEmbed(step),
            )
        ).build()
    )
}

private fun WelcomeByeGuild.createPreviewEmbed(step: CreateStep): net.dv8tion.jda.api.entities.MessageEmbed = createMemberEmbed(
    data = step.data,
    user = step.previewUser,
    guildName = step.previewGuildName,
    memberCount = step.previewMemberCount,
    isJoin = step.previewType == PreviewType.JOIN,
)

private fun WelcomeByeGuild.ensureDefaults(data: GuildSetting, locale: DiscordLocale) {
    val defaultJoin = defaultTemplate(WelcomeByeGuild.MessageKeys.DEFAULT_JOIN, locale)
    val defaultLeave = defaultTemplate(WelcomeByeGuild.MessageKeys.DEFAULT_LEAVE, locale)

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

private fun WelcomeByeGuild.defaultTemplate(key: String, locale: DiscordLocale): Template = messageCreator
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

private fun WelcomeByeGuild.createMessage(
    key: String,
    locale: DiscordLocale,
    replaceMap: Map<String, String> = emptyMap(),
) = messageCreator.getCreateBuilder(key, locale, replaceMap).build()
