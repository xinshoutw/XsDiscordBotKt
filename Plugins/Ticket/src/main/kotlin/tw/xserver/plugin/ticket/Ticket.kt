package tw.xserver.plugin.ticket

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import tw.xserver.loader.builtin.messagecreator.MessageCreator
import tw.xserver.loader.builtin.messagecreator.ModalCreator
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.loader.json.guild.JsonObjGuildFileManager
import tw.xserver.loader.util.ComponentField
import tw.xserver.loader.util.ComponentIdManager
import tw.xserver.loader.util.FieldType
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.ticket.Event.COMPONENT_PREFIX
import tw.xserver.plugin.ticket.Event.PLUGIN_DIR_FILE
import tw.xserver.plugin.ticket.create.StepManager
import tw.xserver.plugin.ticket.json.serializer.JsonDataClass
import java.io.File

internal object Ticket {
    val gson = Gson()
    val jsonGuildManager = JsonObjGuildFileManager(File(PLUGIN_DIR_FILE, "data"))
    val componentIdManager = ComponentIdManager(
        prefix = COMPONENT_PREFIX,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "sub_action" to FieldType.STRING,
            "color_index" to FieldType.STRING,
            "user_id" to FieldType.LONG_HEX, // lock / unlock
            "msg_id" to FieldType.STRING,
            "btn_index" to FieldType.STRING,
        )
    )
    val messageCreator = MessageCreator(
        langDirFile = File(PLUGIN_DIR_FILE, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
        messageKeys = listOf(
            "add-ticket",
            "confirm-add",
            "confirm-create",
            "create-ticket",
            "modify-admin-role",
            "modify-category",
            "modify-btn-color",
        )
    )
    val modalCreator = ModalCreator(
        langDirFile = File(PLUGIN_DIR_FILE, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
        modalKeys = listOf(
            "modify-author",
            "modify-content",
            "modify-btn-text",
            "modify-embed-color",
            "modify-reason-title",
            "preview-reason",
            "press-ticket",
        )
    )

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        StepManager.onSlashCommandInteraction(event)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        GlobalUtil.checkComponentIdPrefix(event, COMPONENT_PREFIX)
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
                    .toClass<JsonDataClass>(object : TypeToken<JsonDataClass>() {}.type)
                    .get(event.messageId)
                    ?.get((idMap["btn_index"] as String).toInt())
                    ?.reasonTitle
                    ?: { throw IllegalStateException("Cannot find data.") }()

                event.replyModal(
                    modalCreator.getModalBuilder(
                        "press-ticket",
                        event.userLocale,
                        substitutor = Placeholder.getSubstitutor(event).putAll(
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
                                        ComponentField("action", "unlock"),
                                        ComponentField("user_id", idMap["user_id"] as Long),
                                        ComponentField("msg_id", idMap["msg_id"] as String),
                                        ComponentField("btn_index", idMap["btn_index"] as String),
                                    ), "開啟", Emoji.fromUnicode("🔓")
                                ),
                                Button.of(
                                    ButtonStyle.DANGER,
                                    componentIdManager.build(
                                        ComponentField("action", "delete"),
                                        ComponentField("user_id", idMap["user_id"] as Long),
                                        ComponentField("msg_id", idMap["msg_id"] as String),
                                        ComponentField("btn_index", idMap["btn_index"] as String),
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
                                        ComponentField("action", "lock"),
                                        ComponentField("user_id", idMap["user_id"] as Long),
                                        ComponentField("msg_id", idMap["msg_id"] as String),
                                        ComponentField("btn_index", idMap["btn_index"] as String),
                                    ), "關閉", Emoji.fromUnicode("🔒")
                                ),
                                Button.of(
                                    ButtonStyle.DANGER,
                                    componentIdManager.build(
                                        ComponentField("action", "delete"),
                                        ComponentField("user_id", idMap["user_id"] as Long),
                                        ComponentField("msg_id", idMap["msg_id"] as String),
                                        ComponentField("btn_index", idMap["btn_index"] as String),
                                    ), "刪除", Emoji.fromUnicode("🗑")
                                )
                            )
                        )
                    }.queue()
            }

            "delete" -> {
                val member = event.member!!
                val jsonData =
                    jsonGuildManager[guild.idLong].getAsJsonArray(idMap["msg_id"] as String)[(idMap["btn_index"] as String).toInt()].asJsonObject
                val roleIds = jsonData.getAsJsonArray("adminIds").map { it.asLong }
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
        val jsonData =
            jsonGuildManager[guild.idLong].getAsJsonArray(idMap["msg_id"] as String)[(idMap["btn_index"] as String).toInt()].asJsonObject
        val reason = event.getValue("reason")!!.asString
        val roleIds = jsonData.getAsJsonArray("adminIds").toList().map { it.asLong }
        val categoryId = jsonData.get("categoryId").asLong
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
                        ComponentField("action", "lock"),
                        ComponentField("user_id", event.user.idLong),
                        ComponentField("msg_id", idMap["msg_id"] as String),
                        ComponentField("btn_index", idMap["btn_index"] as String),
                    ), "關閉", Emoji.fromUnicode("🔒")
                ),
                Button.of(
                    ButtonStyle.DANGER,
                    componentIdManager.build(
                        ComponentField("action", "delete"),
                        ComponentField("user_id", event.user.idLong),
                        ComponentField("msg_id", idMap["msg_id"] as String),
                        ComponentField("btn_index", idMap["btn_index"] as String),
                    ), "刪除", Emoji.fromUnicode("🗑")
                )
            )
        }.queue()
    }
}
