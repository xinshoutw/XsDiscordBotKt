package tw.xinshou.plugin.feedbacker

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import tw.xinshou.loader.builtin.messagecreator.MessageCreator
import tw.xinshou.loader.builtin.messagecreator.ModalCreator
import tw.xinshou.loader.builtin.placeholder.Placeholder
import tw.xinshou.loader.util.ComponentIdManager
import tw.xinshou.loader.util.FieldType
import tw.xinshou.plugin.feedbacker.Event.componentPrefix
import tw.xinshou.plugin.feedbacker.Event.config
import tw.xinshou.plugin.feedbacker.Event.globalLocale
import tw.xinshou.plugin.feedbacker.Event.pluginDirectory
import java.io.File
import java.util.*

internal object Feedbacker {
    private val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "user_id" to FieldType.STRING, // placeholder
            "star_count" to FieldType.STRING, // placeholder
        )
    )

    private val messageCreator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
    )

    private val modalCreator = ModalCreator(
        langDirFile = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
    )

    lateinit var guild: Guild
    lateinit var submitChannel: TextChannel

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild || event.guild!!.idLong != config.guildId) return
        val member = event.member ?: return
        if (Collections.disjoint(config.allowRoleId, member.roles.map { it.idLong })) {
            event.hook.editOriginal(config.formNoPermission).queue()
            return
        }


        event.channel.sendMessage(
            messageCreator.getCreateBuilder(
                key = "ask-message",
                locale = event.userLocale,
                substitutor = Placeholder.get(event.getOption("member")!!.asMember!!)
            ).build()
        ).flatMap {
            event.hook.editOriginal(config.formSuccess)
        }.queue()
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["action"]) {
            "submit_star" -> handleStarBtn(event, idMap)
            "fill_form" -> handleFormBtn(event, idMap)
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

        val newComponents = event.message.components.map { actionRow ->
            ActionRow.of(
                actionRow.components.map { component ->
                    when (component) {
                        is Button -> component.withStyle(
                            if (component.id == event.componentId) ButtonStyle.PRIMARY
                            else ButtonStyle.SECONDARY
                        )

                        else -> component
                    }
                }
            )
        }

        event.deferEdit().flatMap {
            event.hook.editOriginalComponents(newComponents)
        }.queue()
    }


    fun handleFormBtn(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        if (idMap["user_id"] as String != event.user.id) {
            event.reply(config.formNotYou).setEphemeral(true).queue()
            return
        }

        val locale = event.userLocale
        var stars: Int = -1
        event.message.components[0].forEachIndexed { index, button ->
            if ((button as Button).style == ButtonStyle.PRIMARY) {
                stars = index + 1
                return@forEachIndexed
            }
        }

        if (stars == -1) {
            event.reply(config.formWarning).setEphemeral(true).queue()
            return
        }

        event.replyModal(
            modalCreator.getModalBuilder(
                key = "fill-form",
                locale = locale,
                substitutor = Placeholder.get(event).put("fb_star_count", stars.toString())
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


        event.deferEdit().flatMap {
            submitChannel.sendMessage(
                messageCreator.getCreateBuilder("print-result", globalLocale, substitutor).build()
            )
        }.queue()
    }
}
