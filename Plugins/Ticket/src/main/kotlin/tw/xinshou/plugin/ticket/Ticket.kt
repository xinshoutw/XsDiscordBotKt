package tw.xinshou.plugin.ticket

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.Permission.ADMINISTRATOR
import net.dv8tion.jda.api.Permission.VIEW_CHANNEL
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import tw.xinshou.core.builtin.messagecreator.MessageCreator
import tw.xinshou.core.builtin.messagecreator.ModalCreator
import tw.xinshou.core.builtin.placeholder.Placeholder
import tw.xinshou.core.json.JsonFileManager
import tw.xinshou.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.core.json.JsonGuildFileManager
import tw.xinshou.core.util.ComponentIdManager
import tw.xinshou.core.util.FieldType
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.ticket.Event.componentPrefix
import tw.xinshou.plugin.ticket.Event.pluginDirectory
import tw.xinshou.plugin.ticket.create.StepManager
import tw.xinshou.plugin.ticket.json.serializer.JsonDataClass
import java.io.File

internal object Ticket {
    val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()
    val jsonGuildManager = JsonGuildFileManager<JsonDataClass>(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = mutableMapOf()
    )

    val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "sub_action" to FieldType.STRING,
            "color_index" to FieldType.STRING,
            "user_id" to FieldType.LONG_HEX, // lock / unlock
            "msg_id" to FieldType.STRING,
            "btn_index" to FieldType.STRING,
        )
    )
    var messageCreator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager
    )
    var modalCreator = ModalCreator(
        langDirFile = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager
    )

    internal fun reload() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager
        )

        modalCreator = ModalCreator(
            langDirFile = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        StepManager.onSlashCommandInteraction(event)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        GlobalUtil.checkComponentIdPrefix(event, componentPrefix)
        val idMap = componentIdManager.parse(event.componentId)
        val guild = event.guild!!

        when (idMap["action"]) {
            // Trying to create or add ticket buttons
            "create" -> {
                StepManager.onButtonInteraction(event, idMap)
            }

            // Run ticket action
            "press" -> {
                val reason = jsonGuildManager
                    .get(guild.idLong)
                    .data
                    .get(event.messageId)
                    ?.get((idMap["btn_index"] as String).toInt())
                    ?.reasonTitle
                    ?: { throw IllegalStateException("Cannot find data.") }()

                event.replyModal(
                    modalCreator.getModalBuilder(
                        "press-ticket",
                        event.userLocale,
                        substitutor = Placeholder.get(event).putAll(
                            mapOf(
                                "tt@msg-id" to event.messageId,
                                "tt@btn-index" to idMap["btn_index"] as String,
                                "tt@reason" to reason
                            )
                        )
                    ).build()
                ).queue()
            }

            "lock" -> {
                val channel = event.guildChannel.asTextChannel()
                guild.retrieveMemberById(idMap["user_id"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).deny(VIEW_CHANNEL) }
                    .flatMap {
                        event.editComponents(
                            ActionRow.of(
                                Button.of(
                                    ButtonStyle.SUCCESS,
                                    componentIdManager.build(
                                        mapOf(
                                            "action" to "unlock",
                                            "user_id" to idMap["user_id"] as Long,
                                            "msg_id" to idMap["msg_id"] as String,
                                            "btn_index" to idMap["btn_index"] as String,
                                        )
                                    ), "開啟", Emoji.fromUnicode("🔓")
                                ),
                                Button.of(
                                    ButtonStyle.DANGER,
                                    componentIdManager.build(
                                        mapOf(
                                            "action" to "delete",
                                            "user_id" to idMap["user_id"] as Long,
                                            "msg_id" to idMap["msg_id"] as String,
                                            "btn_index" to idMap["btn_index"] as String,
                                        )
                                    ), "刪除", Emoji.fromUnicode("🗑")
                                )
                            )
                        )
                    }.queue()
            }

            "unlock" -> {
                val channel = event.guildChannel.asTextChannel()

                guild.retrieveMemberById(idMap["user_id"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).grant(VIEW_CHANNEL) }
                    .flatMap {
                        event.editComponents(
                            ActionRow.of(
                                Button.of(
                                    ButtonStyle.SECONDARY,
                                    componentIdManager.build(
                                        mapOf(
                                            "action" to "lock",
                                            "user_id" to idMap["user_id"] as Long,
                                            "msg_id" to idMap["msg_id"] as String,
                                            "btn_index" to idMap["btn_index"] as String,
                                        )
                                    ), "關閉", Emoji.fromUnicode("🔒")
                                ),
                                Button.of(
                                    ButtonStyle.DANGER,
                                    componentIdManager.build(
                                        mapOf(
                                            "action" to "delete",
                                            "user_id" to idMap["user_id"] as Long,
                                            "msg_id" to idMap["msg_id"] as String,
                                            "btn_index" to idMap["btn_index"] as String,
                                        )
                                    ), "刪除", Emoji.fromUnicode("🗑")
                                )
                            )
                        )
                    }.queue()
            }

            "delete" -> {
                val member = event.member!!
                val jsonData = jsonGuildManager
                    .get(guild.idLong)
                    .data
                    .get((idMap["msg_id"] as String))!!
                    .get((idMap["btn_index"] as String).toInt())
                val roleIds = jsonData.adminIds
                val isAdmin = member.roles.any { roleIds.contains(it.idLong) }

                if (isAdmin || member.hasPermission(ADMINISTRATOR)) {
                    event.deferEdit().flatMap {
                        event.guildChannel.asTextChannel().delete()
                    }.queue()
                } else {
                    event.deferEdit().queue()
                }
            }
        }
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        StepManager.onEntitySelectInteraction(event)
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        StepManager.onGuildLeave(event)
    }

    fun onModalInteraction(event: ModalInteractionEvent) {
        val idMap = componentIdManager.parse(event.modalId)

        if (idMap["action"] == "create") {
            StepManager.onModalInteraction(event, idMap)
            return
        }

        if (idMap["action"] != "submit") return
        event.deferReply(true).queue()

        val guild = event.guild!!
        val jsonData = jsonGuildManager
            .get(guild.idLong)
            .data
            .get((idMap["msg_id"] as String))!!
            .get((idMap["btn_index"] as String).toInt())

        val reason = event.getValue("reason")!!.asString
        val roleIds = jsonData.adminIds
        val categoryId = jsonData.categoryId
        val category: Category? = if (categoryId == 0L) {
            event.guildChannel.asTextChannel().parentCategory
        } else {
            guild.getCategoryById(categoryId)
        }

        if (category == null) {
            event.reply("錯誤 (無法取得目錄)").setEphemeral(true).queue()
            return
        }

        val builder = StringBuilder()
        category.createTextChannel(event.user.name).apply {
            addPermissionOverride(
                guild.publicRole,
                Permission.getRaw(),
                VIEW_CHANNEL.rawValue
            )
            addMemberPermissionOverride(
                event.user.idLong,
                VIEW_CHANNEL.rawValue,
                Permission.getRaw()
            )

            for (roleId in roleIds) {
                addRolePermissionOverride(roleId, VIEW_CHANNEL.rawValue, Permission.getRaw())
                builder.append("<@&").append(roleId).append("> ")
            }
            builder.append("\n\n").append(reason)
        }.flatMap {
            event.hook.sendMessage("請到此頻道 <#${it.id}> 並等待人員回覆繼續!").queue()

            it.sendMessage(builder.toString()).addActionRow(
                Button.of(
                    ButtonStyle.SECONDARY,
                    componentIdManager.build(
                        mapOf(
                            "action" to "lock",
                            "user_id" to event.user.idLong,
                            "msg_id" to idMap["msg_id"] as String,
                            "btn_index" to idMap["btn_index"] as String,
                        )
                    ), "關閉", Emoji.fromUnicode("🔒")
                ),
                Button.of(
                    ButtonStyle.DANGER,
                    componentIdManager.build(
                        mapOf(
                            "action" to "delete",
                            "user_id" to event.user.idLong,
                            "msg_id" to idMap["msg_id"] as String,
                            "btn_index" to idMap["btn_index"] as String,
                        )
                    ), "刪除", Emoji.fromUnicode("🗑")
                )
            )
        }.queue()
    }
}
