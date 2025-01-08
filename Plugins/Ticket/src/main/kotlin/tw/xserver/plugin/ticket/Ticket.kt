package tw.xserver.plugin.ticket

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
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import tw.xserver.loader.json.guild.JsonObjGuildFileManager
import tw.xserver.loader.util.ComponentField
import tw.xserver.loader.util.ComponentIdManager
import tw.xserver.loader.util.FieldType
import tw.xserver.loader.util.GlobalUtil
import tw.xserver.plugin.ticket.Event.COMPONENT_PREFIX
import tw.xserver.plugin.ticket.Event.PLUGIN_DIR_FILE
import tw.xserver.plugin.ticket.create.StepManager
import java.io.File

object Ticket {
    internal val jsonGuildManager = JsonObjGuildFileManager(File(PLUGIN_DIR_FILE, "data"))
    internal val componentIdManager = ComponentIdManager(
        "ticket-v2", mapOf(
            "action" to FieldType.STRING,
            "subAction" to FieldType.STRING,
            "userId" to FieldType.LONG_HEX,
            "msgId" to FieldType.LONG_HEX,
            "btnIndex" to FieldType.INT_HEX,
        )
    )

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        StepManager.onSlashCommandInteraction(event)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        GlobalUtil.checkPrefix(event, COMPONENT_PREFIX) // TODO: add '@' check
        val idMap = componentIdManager.parse(event.componentId)
        val guild = event.guild!!

        when (idMap["action"]) {
            "create" -> {
                StepManager.onButtonInteraction(event, idMap)
            }

            "press" -> {
                val jsonManager = jsonGuildManager[guild.idLong]
                val jsonData =
                    jsonManager.getAsJsonArray(event.messageId)[idMap["btnIndex"] as Int].asJsonObject
                val reason = jsonData.asJsonObject.get("reasonTitle").asString
                val reasonInput = TextInput.create("reason", "原因", TextInputStyle.PARAGRAPH).build()

                event.replyModal(
                    Modal.create(
                        componentIdManager.build(
                            ComponentField("action", "submit"),
                            ComponentField("msgId", event.messageIdLong),
                            ComponentField("btnIndex", idMap["btnIndex"] as Int),
                        ), reason
                    ).addComponents(ActionRow.of(reasonInput)).build()
                ).queue()
            }

            "lock" -> {
                val channel = event.guildChannel.asTextChannel()
                guild.retrieveMemberById(idMap["userId"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).deny(VIEW_CHANNEL) }
                    .queue()

                event.editComponents(
                    ActionRow.of(
                        Button.of(
                            ButtonStyle.SUCCESS,
                            componentIdManager.build(
                                ComponentField("action", "unlock"),
                                ComponentField("userId", idMap["userId"] as Long),
                                ComponentField("msgId", idMap["msgId"] as Long),
                                ComponentField("btnIndex", idMap["btnIndex"] as Int),
                            ), "開啟", Emoji.fromUnicode("🔓")
                        ),
                        Button.of(
                            ButtonStyle.DANGER,
                            componentIdManager.build(
                                ComponentField("action", "delete"),
                                ComponentField("userId", idMap["userId"] as Long),
                                ComponentField("msgId", idMap["msgId"] as Long),
                                ComponentField("btnIndex", idMap["btnIndex"] as Int),
                            ), "刪除", Emoji.fromUnicode("🗑")
                        )
                    )
                ).queue()
            }

            "unlock" -> {
                val channel = event.guildChannel.asTextChannel()

                guild.retrieveMemberById(idMap["userId"] as Long)
                    .flatMap { channel.upsertPermissionOverride(it).grant(VIEW_CHANNEL) }
                    .queue()

                event.editComponents(
                    ActionRow.of(
                        Button.of(
                            ButtonStyle.SUCCESS,
                            componentIdManager.build(
                                ComponentField("action", "lock"),
                                ComponentField("userId", idMap["userId"] as Long),
                                ComponentField("msgId", idMap["msgId"] as Long),
                                ComponentField("btnIndex", idMap["btnIndex"] as Int),
                            ), "關閉", Emoji.fromUnicode("🔒")
                        ),
                        Button.of(
                            ButtonStyle.DANGER,
                            componentIdManager.build(
                                ComponentField("action", "delete"),
                                ComponentField("userId", idMap["userId"] as Long),
                                ComponentField("msgId", idMap["msgId"] as Long),
                                ComponentField("btnIndex", idMap["btnIndex"] as Int),
                            ), "刪除", Emoji.fromUnicode("🗑")
                        )
                    )
                ).queue()
            }

            "delete" -> {
                val member = event.member!!
                val jsonData =
                    jsonGuildManager[guild.idLong].getAsJsonArray((idMap["msgId"] as Long).toString())[idMap["btnIndex"] as Int].asJsonObject
                val roleIds = jsonData.getAsJsonArray("adminIds").map { it.asLong }
                val isAdmin = member.roles.any { roleIds.contains(it.idLong) }

                event.deferEdit().queue()
                if (isAdmin || member.hasPermission(ADMINISTRATOR)) {
                    event.guildChannel.asTextChannel().delete().queue()
                }
            }
        }
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        StepManager.onEntitySelectInteraction(event)
    }

    fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        StepManager.onStringSelectInteraction(event)
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
            jsonGuildManager[guild.idLong].getAsJsonArray((idMap["msgId"] as Long).toString())[idMap["btnIndex"] as Int].asJsonObject
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
                    ButtonStyle.SUCCESS,
                    componentIdManager.build(
                        ComponentField("action", "lock"),
                        ComponentField("userId", event.user.idLong),
                        ComponentField("msgId", idMap["msgId"] as Long),
                        ComponentField("btnIndex", idMap["btnIndex"] as Int),
                    ), "關閉", Emoji.fromUnicode("🔒")
                ),
                Button.of(
                    ButtonStyle.DANGER,
                    componentIdManager.build(
                        ComponentField("action", "delete"),
                        ComponentField("userId", event.user.idLong),
                        ComponentField("msgId", idMap["msgId"] as Long),
                        ComponentField("btnIndex", idMap["btnIndex"] as Int),
                    ), "刪除", Emoji.fromUnicode("🗑")
                )
            )
        }.queue()
    }
}
