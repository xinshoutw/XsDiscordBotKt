package tw.xinshou.discord.plugin.feedbacker

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import core.placeholder.withMember
import core.placeholder.withUser
import core.util.ComponentId
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal
import tw.xinshou.discord.plugin.feedbacker.Event.config
import tw.xinshou.discord.plugin.feedbacker.Event.pluginConfig
import tw.xinshou.discord.plugin.feedbacker.Event.pluginDirectory
import java.io.File

internal object Feedbacker {
    private val componentId = ComponentId(
        prefix = config.componentPrefix,
        idKeys = mapOf(
            "action" to ComponentId.FieldType.STRING,
            "user_id" to ComponentId.FieldType.STRING,
            "star_count" to ComponentId.FieldType.STRING,
            "submit_channel_id" to ComponentId.FieldType.STRING,
        )
    )

    private var messageTemplate = MessageTemplate(
        langDir = File(pluginDirectory, "lang"),
        defaultLocale = net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN,
        componentIdPrefix = config.componentPrefix,
    )

    fun reload() {
        messageTemplate = MessageTemplate(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN,
            componentIdPrefix = config.componentPrefix,
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) return
        val member = event.member ?: return
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            event.hook.editOriginal(pluginConfig.formNoPermission).queue()
            return
        }

        val submitChannel = event.getOption("submit-channel")!!.asChannel
        if (submitChannel.type != ChannelType.TEXT) {
            event.hook.editOriginal("SubmitChannel must be TextChannel").queue()
            return
        }

        val targetMember = event.getOption("member")!!.asMember!!
        val substitutor = Substitutor()
            .withUser(targetMember.user)
            .withMember(targetMember)
            .put("fb_submit_channel_id", submitChannel.id)

        event.channel.sendMessage(
            messageTemplate.buildCreate(
                messageId = "ask-message",
                locale = event.userLocale,
                substitutor = substitutor
            ).build()
        ).flatMap {
            event.hook.editOriginal(pluginConfig.formSuccess)
        }.queue()
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val idMap = componentId.parse(event.componentId)
        when (idMap["action"]) {
            "submit_star" -> handleStarBtn(event, idMap)
        }
    }

    fun onModalInteraction(event: ModalInteractionEvent) {
        val idMap = componentId.parse(event.modalId)
        when (idMap["action"]) {
            "submit_form" -> handleFormSubmit(event, idMap)
        }
    }

    private fun handleStarBtn(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        if (idMap["user_id"] as String != event.user.id) {
            event.reply(pluginConfig.formNotYou).setEphemeral(true).queue()
            return
        }

        val newComponents = event.message.components
            .filter { it.type == Component.Type.ACTION_ROW }
            .map { topLevel ->
                val actionRow = topLevel.asActionRow()
                ActionRow.of(
                    actionRow.components.map { component ->
                        when (component) {
                            is Button -> component.withStyle(
                                if (component.customId == event.componentId) ButtonStyle.PRIMARY
                                else ButtonStyle.SECONDARY
                            )

                            else -> component
                        }
                    }
                )
            }

        event.hook.editOriginalComponents(newComponents).queue()

        val starCount = idMap["star_count"] as String
        val submitChannelId = idMap["submit_channel_id"] as String
        val modalId = componentId.build(
            "action" to "submit_form",
            "star_count" to starCount,
            "submit_channel_id" to submitChannelId,
        )

        val modal = Modal.create(modalId, "Feedback Form")
            .addComponents(
                Label.of("Your feedback", TextInput.create("form", TextInputStyle.PARAGRAPH)
                    .setRequired(true)
                    .build())
            )
            .build()

        event.replyModal(modal).queue()
    }

    private fun handleFormSubmit(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val stars = (idMap["star_count"] as String).toInt()
        val substitutor = Substitutor().apply {
            event.member?.let {
                withUser(it.user)
                withMember(it)
            }
            putAll(
                "fb_stars" to "${"★ ".repeat(stars)}${"☆ ".repeat(5 - stars)}",
                "fb_content" to event.getValue("form")!!.asString
            )
        }

        val submitChannel = event.guild?.getTextChannelById(idMap["submit_channel_id"] as String)
        if (submitChannel == null) {
            event.hook.editOriginal("Submit channel not found").queue()
            return
        }

        event.deferEdit().flatMap {
            submitChannel.sendMessage(
                messageTemplate.buildCreate("print-result", event.guildLocale, substitutor).build()
            )
        }.queue()

        event.message?.delete()?.queue()
    }
}
