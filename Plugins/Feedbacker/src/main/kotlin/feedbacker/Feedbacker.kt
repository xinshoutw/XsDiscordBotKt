package tw.xinshou.discord.plugin.feedbacker

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import tw.xinshou.discord.core.builtin.messagecreator.modal.ModalCreator
import tw.xinshou.discord.core.builtin.messagecreator.v2.MessageCreator
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.core.util.ComponentIdManager
import tw.xinshou.discord.core.util.FieldType
import tw.xinshou.discord.plugin.feedbacker.Event.componentPrefix
import tw.xinshou.discord.plugin.feedbacker.Event.config
import tw.xinshou.discord.plugin.feedbacker.Event.pluginDirectory
import java.io.File

internal object Feedbacker {
    private val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "user_id" to FieldType.STRING, // placeholder
            "star_count" to FieldType.STRING, // placeholder
            "submit_channel_id" to FieldType.STRING,
        )
    )

    private var messageCreator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
    )

    private var modalCreator = ModalCreator(
        langDirFile = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
    )

    fun reload() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager,
        )
        modalCreator = ModalCreator(
            langDirFile = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager,
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) return
        val member = event.member ?: return
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            event.hook.editOriginal(config.formNoPermission).queue()
            return
        }

        val submitChannel = event.getOption("submit-channel")!!.asChannel
        if (submitChannel.type != ChannelType.TEXT) {
            event.hook.editOriginal("SubmitChannel must be TextChannel").queue()
            return
        }

        event.channel.sendMessage(
            messageCreator.getCreateBuilder(
                key = "ask-message",
                locale = event.userLocale,
                substitutor = Placeholder.get(event.getOption("member")!!.asMember!!).put(
                    "fb_submit_channel_id" to submitChannel.id
                )
            ).build()
        ).flatMap {
            event.hook.editOriginal(config.formSuccess)
        }.queue()
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["action"]) {
            "submit_star" -> handleStarBtn(event, idMap)
        }
    }

    fun onModalInteraction(event: ModalInteractionEvent) {
        val idMap = componentIdManager.parse(event.modalId)
        when (idMap["action"]) {
            "submit_form" -> handleFormSubmit(event, idMap)
        }
    }

    fun handleStarBtn(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        if (idMap["user_id"] as String != event.user.id) {
            event.reply(config.formNotYou).setEphemeral(true).queue()
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
        event.replyModal(
            modalCreator.getModalBuilder(
                key = "fill-form",
                locale = event.userLocale,
                substitutor = Placeholder.get(event).putAll(
                    "fb_star_count" to idMap["star_count"] as String,
                    "fb_submit_channel_id" to idMap["submit_channel_id"] as String
                )
            ).build()
        ).queue()


    }

    fun handleFormSubmit(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val stars = (idMap["star_count"] as String).toInt()
        val substitutor = (event.member?.let { Placeholder.get(it) } ?: Placeholder.globalSubstitutor)
            .putAll(
                "fb_stars" to "${"★ ".repeat(stars)}${"☆ ".repeat(5 - stars)}",
                "fb_content" to event.getValue("form")!!.asString
            )

        val submitChannel = event.guild?.getTextChannelById(idMap["submit_channel_id"] as String)
        if (submitChannel == null) {
            event.hook.editOriginal("Submit channel not found").queue()
            return
        }

        event.deferEdit().flatMap {
            submitChannel.sendMessage(
                messageCreator.getCreateBuilder("print-result", event.guildLocale, substitutor).build()
            )
        }.queue()

        event.message?.delete()?.queue()
    }
}
