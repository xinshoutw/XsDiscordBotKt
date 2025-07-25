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
import tw.xinshou.plugin.feedbacker.Event.COMPONENT_PREFIX
import tw.xinshou.plugin.feedbacker.Event.PLUGIN_DIR_FILE
import tw.xinshou.plugin.feedbacker.Event.config
import tw.xinshou.plugin.feedbacker.Event.globalLocale
import java.io.File
import java.util.*

internal object Feedbacker {
    private val componentIdManager = ComponentIdManager(
        prefix = COMPONENT_PREFIX,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "user_id" to FieldType.LONG_HEX,
            "star_count" to FieldType.INT_HEX,
        )
    )

    private val messageCreator = MessageCreator(
        langDirFile = File(PLUGIN_DIR_FILE, "lang"),
        componentIdManager = componentIdManager,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
    )

    private val modalCreator = ModalCreator(
        langDirFile = File(PLUGIN_DIR_FILE, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
        modalKeys = listOf(
            "fill-form",
        ),
    )

    lateinit var guild: Guild
    lateinit var submitChannel: TextChannel

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild || event.guild!!.idLong != config.guildId) return
        if (Collections.disjoint(config.allowRoleId, event.member!!.roles.map { it.idLong })) {
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
        if (idMap["user_id"] as Long != event.user.idLong) {
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
        if (idMap["user_id"] as Long != event.user.idLong) {
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
        val stars = idMap["star_count"] as Int
        val substitutor = Placeholder.get(event.member!!)
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
