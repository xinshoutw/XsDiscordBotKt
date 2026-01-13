package tw.xinshou.plugin.giveaway

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.core.builtin.messagecreator.MessageCreator
import tw.xinshou.core.util.ComponentIdManager
import tw.xinshou.core.util.FieldType
import tw.xinshou.plugin.giveaway.Event.componentPrefix
import tw.xinshou.plugin.giveaway.Event.pluginDirectory
import tw.xinshou.plugin.giveaway.data.GiveawayConfig
import tw.xinshou.plugin.giveaway.data.GiveawayInstance
import tw.xinshou.plugin.giveaway.data.RolePermissionType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

internal object Giveaway {
    // https://message.style/app/editor/share/X850kw8C
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // Component ID manager for handling button and select menu interactions
    private val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "giveaway_id" to FieldType.STRING,
            "user_id" to FieldType.STRING,
            "config_type" to FieldType.STRING
        )
    )

    // Message creator for internationalized messages
    private var messageCreator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
    )

    // Storage for active giveaways and temporary configurations
    private val activeGiveaways = ConcurrentHashMap<String, GiveawayInstance>()
    private val tempConfigs = ConcurrentHashMap<String, GiveawayConfig>()

    internal fun reload() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager,
        )
    }

    /**
     * Handle the /create-giveaway slash command
     */
    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) {
            event.reply("This command can only be used in a server!").setEphemeral(true).queue()
            return
        }

        val userId = event.user.id
        val tempConfigId = "temp_${userId}_${System.currentTimeMillis()}"

        // Initialize temporary configuration
        tempConfigs[tempConfigId] = GiveawayConfig()

        // Create the initial configuration interface
        val message = createConfigurationMessage(tempConfigId, GiveawayConfig())

        event.hook.editOriginal(MessageEditBuilder.fromCreateData(message.build()).build()).queue()
    }

    /**
     * Handle button interactions for giveaway configuration and participation
     */
    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["action"]) {
            // Configuration buttons
            "set_giveaway_name" -> handleSetGiveawayNameButton(event, idMap)
            "set_prize_name" -> handleSetPrizeNameButton(event, idMap)
            "set_time" -> handleSetTimeButton(event, idMap)
            "set_winner_count" -> handleSetWinnerCountButton(event, idMap)
            "set_sponsor" -> handleSetSponsorButton(event, idMap)
            "set_thumbnail" -> handleSetThumbnailButton(event, idMap)
            "toggle_role_permission" -> handleToggleRolePermissionButton(event, idMap)
            "toggle_weight_additive" -> handleToggleWeightAdditiveButton(event, idMap)
            "toggle_dm_winners" -> handleToggleDmWinnersButton(event, idMap)
            "set_join_time" -> handleSetJoinTimeButton(event, idMap)
            "select_roles" -> handleSelectRolesButton(event, idMap)
            "set_role_weights" -> handleSetRoleWeightsButton(event, idMap)

            // Action buttons
            "preview" -> handlePreviewButton(event, idMap)
            "create" -> handleCreateButton(event, idMap)
            "participate" -> handleParticipateButton(event, idMap)
            "reroll" -> handleRerollButton(event, idMap)
        }
    }

    /**
     * Handle select menu interactions for role configuration
     */
    fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["action"]) {
            "select_join_time" -> handleJoinTimeSelect(event, idMap)
            "role_permission" -> handleRolePermissionSelect(event, idMap)
            "role_weights" -> handleRoleWeightsSelect(event, idMap)
        }
    }

    /**
     * Handle entity select menu interactions for role selection
     */
    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val idMap = componentIdManager.parse(event.componentId)
        when (idMap["action"]) {
            "entity_select_roles" -> handleEntitySelectRoles(event, idMap)
            "entity_select_role_weights" -> handleEntitySelectRoleWeights(event, idMap)
        }
    }

    /**
     * Handle modal interactions for configuration
     */
    fun onModalInteraction(event: ModalInteractionEvent) {
        val idMap = componentIdManager.parse(event.modalId)
        when (idMap["action"]) {
            "modal_giveaway_name" -> handleGiveawayNameModal(event, idMap)
            "modal_prize_name" -> handlePrizeNameModal(event, idMap)
            "modal_time" -> handleTimeModal(event, idMap)
            "modal_winner_count" -> handleWinnerCountModal(event, idMap)
            "modal_sponsor" -> handleSponsorModal(event, idMap)
            "modal_thumbnail" -> handleThumbnailModal(event, idMap)
            "modal_role_weight" -> handleRoleWeightModal(event, idMap)
        }
    }

    /**
     * Create the initial configuration message with individual configuration buttons
     */
    private fun createConfigurationMessage(tempConfigId: String, config: GiveawayConfig): MessageCreateBuilder {
        val builder = MessageCreateBuilder()

        // Create preview embed
        val previewEmbed = createPreviewEmbed(config)
        builder.setEmbeds(previewEmbed)

        // Create individual configuration buttons
        val giveawayNameButton = Button.of(
            getButtonStyle(config.giveawayName.isNotEmpty(), true),
            componentIdManager.build(
                mapOf(
                    "action" to "set_giveaway_name",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🎯 設定抽獎名稱"
        )

        val prizeNameButton = Button.of(
            getButtonStyle(config.prizeName.isNotEmpty(), true),
            componentIdManager.build(
                mapOf(
                    "action" to "set_prize_name",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🏆 獎品名稱"
        )

        val timeButton = Button.of(
            getButtonStyle(hasTimeConfiguration(config), true),
            componentIdManager.build(
                mapOf(
                    "action" to "set_time",
                    "giveaway_id" to tempConfigId
                )
            ),
            "⏰ 抽獎時間"
        )

        val winnerCountButton = Button.of(
            getButtonStyle(config.winnerCount != 1, false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_winner_count",
                    "giveaway_id" to tempConfigId
                )
            ),
            "👥 中獎人數"
        )

        val rolePermissionButton = Button.of(
            getButtonStyle(config.rolePermissionType != RolePermissionType.ALL_ALLOWED, false),
            componentIdManager.build(
                mapOf(
                    "action" to "toggle_role_permission",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🔒 ${getRolePermissionText(config.rolePermissionType)}"
        )

        val weightAdditiveButton = Button.of(
            getButtonStyle(config.isWeightAdditive, false),
            componentIdManager.build(
                mapOf(
                    "action" to "toggle_weight_additive",
                    "giveaway_id" to tempConfigId
                )
            ),
            "⚖️ 權重疊加"
        )

        // Row 1: Required fields
        val row1 = ActionRow.of(giveawayNameButton, prizeNameButton, timeButton)

        // Row 2: Optional configuration
        val row2 = ActionRow.of(winnerCountButton, rolePermissionButton, weightAdditiveButton)

        // Row 3: Additional settings
        val sponsorButton = Button.of(
            getButtonStyle(config.sponsor.isNotEmpty(), false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_sponsor",
                    "giveaway_id" to tempConfigId
                )
            ),
            "💼 贊助商/主辦方"
        )

        val joinTimeButton = Button.of(
            getButtonStyle(config.serverJoinTimeRequirement != null, false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_join_time",
                    "giveaway_id" to tempConfigId
                )
            ),
            "📅 加入時間要求"
        )

        val dmWinnersButton = Button.of(
            getButtonStyle(config.shouldDmWinners, false),
            componentIdManager.build(
                mapOf(
                    "action" to "toggle_dm_winners",
                    "giveaway_id" to tempConfigId
                )
            ),
            "📨 私訊中獎者"
        )

        val thumbnailButton = Button.of(
            getButtonStyle(config.thumbnailUrl.isNotEmpty(), false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_thumbnail",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🖼️ 縮圖URL"
        )

        val row3 = ActionRow.of(sponsorButton, joinTimeButton, dmWinnersButton)
        val row4 = ActionRow.of(thumbnailButton)

        // Row 5: Role configuration (only show if not ALL_ALLOWED)
        val components = mutableListOf(row1, row2, row3, row4)

        if (config.rolePermissionType != RolePermissionType.ALL_ALLOWED) {
            val roleSelectButton = Button.of(
                getButtonStyle(config.allowedRoles.isNotEmpty() || config.deniedRoles.isNotEmpty(), false),
                componentIdManager.build(
                    mapOf(
                        "action" to "select_roles",
                        "giveaway_id" to tempConfigId
                    )
                ),
                "👤 選擇身份組"
            )

            val roleWeightButton = Button.of(
                getButtonStyle(config.roleWeights.isNotEmpty(), false),
                componentIdManager.build(
                    mapOf(
                        "action" to "set_role_weights",
                        "giveaway_id" to tempConfigId
                    )
                ),
                "⚖️ 身份組權重"
            )

            components.add(ActionRow.of(roleSelectButton, roleWeightButton))
        }

        // Row 5: Action buttons
        val previewButton = Button.primary(
            componentIdManager.build(
                mapOf(
                    "action" to "preview",
                    "giveaway_id" to tempConfigId
                )
            ),
            "👁️ 預覽"
        )

        val createButton = Button.success(
            componentIdManager.build(
                mapOf(
                    "action" to "create",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🎉 建立抽獎"
        ).withDisabled(!isConfigurationValid(config))

        components.add(ActionRow.of(previewButton, createButton))

        builder.setComponents(components)

        return builder
    }

    /**
     * Create the configuration message for editing (returns MessageEditBuilder)
     */
    private fun createConfigurationEditMessage(tempConfigId: String, config: GiveawayConfig): MessageEditBuilder {
        val builder = MessageEditBuilder()

        // Create preview embed
        val previewEmbed = createPreviewEmbed(config)
        builder.setEmbeds(previewEmbed)

        // Create individual configuration buttons (same logic as createConfigurationMessage)
        val giveawayNameButton = Button.of(
            getButtonStyle(config.giveawayName.isNotEmpty(), true),
            componentIdManager.build(
                mapOf(
                    "action" to "set_giveaway_name",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🎯 設定抽獎名稱"
        )

        val prizeNameButton = Button.of(
            getButtonStyle(config.prizeName.isNotEmpty(), true),
            componentIdManager.build(
                mapOf(
                    "action" to "set_prize_name",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🏆 獎品名稱"
        )

        val timeButton = Button.of(
            getButtonStyle(hasTimeConfiguration(config), true),
            componentIdManager.build(
                mapOf(
                    "action" to "set_time",
                    "giveaway_id" to tempConfigId
                )
            ),
            "⏰ 抽獎時間"
        )

        val winnerCountButton = Button.of(
            getButtonStyle(config.winnerCount != 1, false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_winner_count",
                    "giveaway_id" to tempConfigId
                )
            ),
            "👥 中獎人數"
        )

        val rolePermissionButton = Button.of(
            getButtonStyle(config.rolePermissionType != RolePermissionType.ALL_ALLOWED, false),
            componentIdManager.build(
                mapOf(
                    "action" to "toggle_role_permission",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🔒 ${getRolePermissionText(config.rolePermissionType)}"
        )

        val weightAdditiveButton = Button.of(
            getButtonStyle(config.isWeightAdditive, false),
            componentIdManager.build(
                mapOf(
                    "action" to "toggle_weight_additive",
                    "giveaway_id" to tempConfigId
                )
            ),
            "⚖️ 權重疊加"
        )

        // Row 1: Required fields
        val row1 = ActionRow.of(giveawayNameButton, prizeNameButton, timeButton)

        // Row 2: Optional configuration
        val row2 = ActionRow.of(winnerCountButton, rolePermissionButton, weightAdditiveButton)

        // Row 3: Additional settings
        val sponsorButton = Button.of(
            getButtonStyle(config.sponsor.isNotEmpty(), false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_sponsor",
                    "giveaway_id" to tempConfigId
                )
            ),
            "💼 贊助商/主辦方"
        )

        val joinTimeButton = Button.of(
            getButtonStyle(config.serverJoinTimeRequirement != null, false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_join_time",
                    "giveaway_id" to tempConfigId
                )
            ),
            "📅 加入時間要求"
        )

        val dmWinnersButton = Button.of(
            getButtonStyle(config.shouldDmWinners, false),
            componentIdManager.build(
                mapOf(
                    "action" to "toggle_dm_winners",
                    "giveaway_id" to tempConfigId
                )
            ),
            "📨 私訊中獎者"
        )

        val thumbnailButton = Button.of(
            getButtonStyle(config.thumbnailUrl.isNotEmpty(), false),
            componentIdManager.build(
                mapOf(
                    "action" to "set_thumbnail",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🖼️ 縮圖URL"
        )

        val row3 = ActionRow.of(sponsorButton, joinTimeButton, dmWinnersButton)
        val row4 = ActionRow.of(thumbnailButton)

        // Row 5: Role configuration (only show if not ALL_ALLOWED)
        val components = mutableListOf(row1, row2, row3, row4)

        if (config.rolePermissionType != RolePermissionType.ALL_ALLOWED) {
            val roleSelectButton = Button.of(
                getButtonStyle(config.allowedRoles.isNotEmpty() || config.deniedRoles.isNotEmpty(), false),
                componentIdManager.build(
                    mapOf(
                        "action" to "select_roles",
                        "giveaway_id" to tempConfigId
                    )
                ),
                "👤 選擇身份組"
            )

            val roleWeightButton = Button.of(
                getButtonStyle(config.roleWeights.isNotEmpty(), false),
                componentIdManager.build(
                    mapOf(
                        "action" to "set_role_weights",
                        "giveaway_id" to tempConfigId
                    )
                ),
                "⚖️ 身份組權重"
            )

            components.add(ActionRow.of(roleSelectButton, roleWeightButton))
        }

        // Row 5: Action buttons
        val previewButton = Button.primary(
            componentIdManager.build(
                mapOf(
                    "action" to "preview",
                    "giveaway_id" to tempConfigId
                )
            ),
            "👁️ 預覽"
        )

        val createButton = Button.success(
            componentIdManager.build(
                mapOf(
                    "action" to "create",
                    "giveaway_id" to tempConfigId
                )
            ),
            "🎉 建立抽獎"
        ).withDisabled(!isConfigurationValid(config))

        components.add(ActionRow.of(previewButton, createButton))

        builder.setComponents(components)

        return builder
    }

    /**
     * Get button style based on configuration status
     */
    private fun getButtonStyle(isConfigured: Boolean, isRequired: Boolean): ButtonStyle {
        return when {
            isConfigured -> ButtonStyle.SUCCESS // Green when configured
            isRequired -> ButtonStyle.DANGER    // Red for required unfilled
            else -> ButtonStyle.SECONDARY       // Gray for optional unfilled
        }
    }

    /**
     * Check if time configuration is set
     */
    private fun hasTimeConfiguration(config: GiveawayConfig): Boolean {
        return config.endTime != null || config.startTime != null || config.duration != null
    }

    /**
     * Get role permission type display text
     */
    private fun getRolePermissionText(type: RolePermissionType): String {
        return when (type) {
            RolePermissionType.ALL_ALLOWED -> "全部允許"
            RolePermissionType.PARTIAL_ALLOWED -> "部分允許"
            RolePermissionType.PARTIAL_DENIED -> "部分拒絕"
        }
    }

    /**
     * Check if configuration is valid for creating giveaway
     */
    private fun isConfigurationValid(config: GiveawayConfig): Boolean {
        return config.giveawayName.isNotEmpty() && config.prizeName.isNotEmpty() && hasTimeConfiguration(config)
    }

    /**
     * Create preview embed for the giveaway configuration
     */
    private fun createPreviewEmbed(config: GiveawayConfig): net.dv8tion.jda.api.entities.MessageEmbed {
        return net.dv8tion.jda.api.EmbedBuilder()
            .setTitle(if (config.giveawayName.isNotEmpty()) "🎉 ${config.giveawayName}" else "🎉 抽獎預覽")
            .setColor(0x00FF00)
            .addField("獎品", config.prizeName.ifEmpty { "未設定" }, true)
            .addField("中獎人數", config.winnerCount.toString(), true)
            .addField("結束時間", "<t:${config.getCalculatedEndTime().epochSecond}:R>", true)
            .addField(
                "參與權限", when (config.rolePermissionType) {
                    RolePermissionType.ALL_ALLOWED -> "所有人"
                    RolePermissionType.PARTIAL_ALLOWED -> "部分允許"
                    RolePermissionType.PARTIAL_DENIED -> "部分拒絕"
                }, true
            )
            .addField("權重疊加", if (config.isWeightAdditive) "啟用" else "停用", true)
            .addField("私訊中獎者", if (config.shouldDmWinners) "是" else "否", true)
            .apply {
                if (config.sponsor.isNotEmpty()) {
                    addField("贊助商", config.sponsor, true)
                }
            }
            .setFooter("使用設定按鈕來修改配置")
            .build()
    }

    // Modal handlers for text input configurations
    private fun handleSetGiveawayNameButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val modal = Modal.create(
            componentIdManager.build(
                mapOf(
                    "action" to "modal_giveaway_name",
                    "giveaway_id" to tempConfigId
                )
            ),
            "設定抽獎名稱"
        ).addComponents(
            ActionRow.of(
                TextInput.create("giveaway_name", "抽獎名稱", TextInputStyle.SHORT)
                    .setPlaceholder("請輸入抽獎名稱...")
                    .setValue(config.giveawayName)
                    .setRequired(true)
                    .setMaxLength(100)
                    .build()
            )
        ).build()

        event.replyModal(modal).queue()
    }

    private fun handleSetPrizeNameButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val modal = Modal.create(
            componentIdManager.build(
                mapOf(
                    "action" to "modal_prize_name",
                    "giveaway_id" to tempConfigId
                )
            ),
            "設定獎品名稱"
        ).addComponents(
            ActionRow.of(
                TextInput.create("prize_name", "獎品名稱", TextInputStyle.SHORT)
                    .setPlaceholder("請輸入獎品名稱...")
                    .setValue(config.prizeName)
                    .setRequired(true)
                    .setMaxLength(100)
                    .build()
            )
        ).build()

        event.replyModal(modal).queue()
    }

    private fun handleSetTimeButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val modal = Modal.create(
            componentIdManager.build(
                mapOf(
                    "action" to "modal_time",
                    "giveaway_id" to tempConfigId
                )
            ),
            "設定抽獎時間"
        ).addComponents(
            ActionRow.of(
                TextInput.create("end_time", "結束時間 (優先級最高)", TextInputStyle.SHORT)
                    .setPlaceholder("格式: 2024-12-31 23:59 或留空")
                    .setValue(config.endTime?.toString())
                    .setRequired(false)
                    .build()
            ),
            ActionRow.of(
                TextInput.create("start_time", "開始時間", TextInputStyle.SHORT)
                    .setPlaceholder("格式: 2024-12-31 12:00 或留空")
                    .setValue(config.startTime?.toString())
                    .setRequired(false)
                    .build()
            ),
            ActionRow.of(
                TextInput.create("duration", "持續時間 (秒)", TextInputStyle.SHORT)
                    .setPlaceholder("例如: 3600 (1小時) 或留空")
                    .setValue(config.duration?.toString())
                    .setRequired(false)
                    .build()
            )
        ).build()

        event.replyModal(modal).queue()
    }

    private fun handleSetWinnerCountButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val modal = Modal.create(
            componentIdManager.build(
                mapOf(
                    "action" to "modal_winner_count",
                    "giveaway_id" to tempConfigId
                )
            ),
            "設定中獎人數"
        ).addComponents(
            ActionRow.of(
                TextInput.create("winner_count", "中獎人數", TextInputStyle.SHORT)
                    .setPlaceholder("請輸入中獎人數 (預設: 1)")
                    .setValue(config.winnerCount.toString())
                    .setRequired(true)
                    .setMaxLength(3)
                    .build()
            )
        ).build()

        event.replyModal(modal).queue()
    }

    private fun handleSetSponsorButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return
        val serverName = event.guild?.name ?: ""

        val modal = Modal.create(
            componentIdManager.build(
                mapOf(
                    "action" to "modal_sponsor",
                    "giveaway_id" to tempConfigId
                )
            ),
            "設定贊助商"
        ).addComponents(
            ActionRow.of(
                TextInput.create("sponsor", "贊助商", TextInputStyle.SHORT)
                    .setPlaceholder("贊助商名稱 (預設: $serverName)")
                    .setValue(config.sponsor.ifEmpty { serverName })
                    .setRequired(false)
                    .setMaxLength(100)
                    .build()
            )
        ).build()

        event.replyModal(modal).queue()
    }

    private fun handleSetThumbnailButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val modal = Modal.create(
            componentIdManager.build(
                mapOf(
                    "action" to "modal_thumbnail",
                    "giveaway_id" to tempConfigId
                )
            ),
            "設定縮圖URL"
        ).addComponents(
            ActionRow.of(
                TextInput.create("thumbnail_url", "縮圖URL", TextInputStyle.SHORT)
                    .setPlaceholder("請輸入圖片URL (可選)")
                    .setValue(config.thumbnailUrl)
                    .setRequired(false)
                    .setMaxLength(500)
                    .build()
            )
        ).build()

        event.replyModal(modal).queue()
    }

    // Toggle button handlers
    private fun handleToggleRolePermissionButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val newType = when (config.rolePermissionType) {
            RolePermissionType.ALL_ALLOWED -> RolePermissionType.PARTIAL_ALLOWED
            RolePermissionType.PARTIAL_ALLOWED -> RolePermissionType.PARTIAL_DENIED
            RolePermissionType.PARTIAL_DENIED -> RolePermissionType.ALL_ALLOWED
        }

        tempConfigs[tempConfigId] = config.copy(rolePermissionType = newType)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleToggleWeightAdditiveButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        tempConfigs[tempConfigId] = config.copy(isWeightAdditive = !config.isWeightAdditive)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleToggleDmWinnersButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        tempConfigs[tempConfigId] = config.copy(shouldDmWinners = !config.shouldDmWinners)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    // Select menu handlers
    private fun handleSetJoinTimeButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String

        val selectMenu = StringSelectMenu.create(
            componentIdManager.build(
                mapOf(
                    "action" to "select_join_time",
                    "giveaway_id" to tempConfigId
                )
            )
        ).setPlaceholder("選擇加入伺服器時間要求")
            .addOptions(
                SelectOption.of("5分鐘", "300"),
                SelectOption.of("10分鐘", "600"),
                SelectOption.of("30分鐘", "1800"),
                SelectOption.of("1小時", "3600"),
                SelectOption.of("3小時", "10800"),
                SelectOption.of("6小時", "21600"),
                SelectOption.of("12小時", "43200"),
                SelectOption.of("1天", "86400"),
                SelectOption.of("3天", "259200"),
                SelectOption.of("7天", "604800"),
                SelectOption.of("14天", "1209600"),
                SelectOption.of("30天", "2592000"),
                SelectOption.of("90天", "7776000"),
                SelectOption.of("180天", "15552000"),
                SelectOption.of("365天", "31536000")
            ).build()

        event.reply("請選擇加入伺服器時間要求:")
            .addComponents(ActionRow.of(selectMenu))
            .setEphemeral(true)
            .queue()
    }

    private fun handleSelectRolesButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String

        val entitySelectMenu = EntitySelectMenu.create(
            componentIdManager.build(
                mapOf(
                    "action" to "entity_select_roles",
                    "giveaway_id" to tempConfigId
                )
            ),
            EntitySelectMenu.SelectTarget.ROLE
        ).setPlaceholder("選擇身份組")
            .setRequiredRange(1, 25)
            .build()

        event.reply("請選擇要配置的身份組:")
            .addComponents(ActionRow.of(entitySelectMenu))
            .setEphemeral(true)
            .queue()
    }

    private fun handleSetRoleWeightsButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String

        val entitySelectMenu = EntitySelectMenu.create(
            componentIdManager.build(
                mapOf(
                    "action" to "entity_select_role_weights",
                    "giveaway_id" to tempConfigId
                )
            ),
            EntitySelectMenu.SelectTarget.ROLE
        ).setPlaceholder("選擇要設定權重的身份組")
            .setRequiredRange(1, 1)
            .build()

        event.reply("請選擇要設定權重的身份組:")
            .addComponents(ActionRow.of(entitySelectMenu))
            .setEphemeral(true)
            .queue()
    }

    // Modal submission handlers
    private fun handleGiveawayNameModal(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val giveawayName = event.getValue("giveaway_name")?.asString?.trim() ?: ""

        if (giveawayName.isEmpty()) {
            event.reply("抽獎名稱不能為空！").setEphemeral(true).queue()
            return
        }

        tempConfigs[tempConfigId] = config.copy(giveawayName = giveawayName)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handlePrizeNameModal(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val prizeName = event.getValue("prize_name")?.asString?.trim() ?: ""

        if (prizeName.isEmpty()) {
            event.reply("獎品名稱不能為空！").setEphemeral(true).queue()
            return
        }

        tempConfigs[tempConfigId] = config.copy(prizeName = prizeName)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleTimeModal(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val endTimeStr = event.getValue("end_time")?.asString?.trim()
        val startTimeStr = event.getValue("start_time")?.asString?.trim()
        val durationStr = event.getValue("duration")?.asString?.trim()

        var endTime: Instant? = null
        var startTime: Instant? = null
        var duration: Long? = null

        try {
            // Parse end time (highest priority)
            if (!endTimeStr.isNullOrEmpty()) {
                endTime = parseDateTime(endTimeStr)
            }

            // Parse start time
            if (!startTimeStr.isNullOrEmpty()) {
                startTime = parseDateTime(startTimeStr)
            }

            // Parse duration
            if (!durationStr.isNullOrEmpty()) {
                duration = durationStr.toLongOrNull()
                if (duration != null && duration <= 0) {
                    event.reply("持續時間必須大於0秒！").setEphemeral(true).queue()
                    return
                }
            }

            // Validate that at least one time setting is provided
            if (endTime == null && startTime == null && duration == null) {
                event.reply("請至少設定一個時間選項！").setEphemeral(true).queue()
                return
            }

            tempConfigs[tempConfigId] = config.copy(
                endTime = endTime,
                startTime = startTime,
                duration = duration
            )

            val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
            event.deferEdit().flatMap { _ ->
                event.hook.editOriginal(updatedMessage.build())
            }.queue()

        } catch (e: Exception) {
            event.reply("時間格式錯誤！請使用格式: YYYY-MM-DD HH:MM").setEphemeral(true).queue()
        }
    }

    private fun handleWinnerCountModal(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val winnerCountStr = event.getValue("winner_count")?.asString?.trim() ?: "1"
        val winnerCount = winnerCountStr.toIntOrNull()

        if (winnerCount == null || winnerCount <= 0) {
            event.reply("中獎人數必須是大於0的整數！").setEphemeral(true).queue()
            return
        }

        tempConfigs[tempConfigId] = config.copy(winnerCount = winnerCount)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleSponsorModal(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val sponsor = event.getValue("sponsor")?.asString?.trim() ?: ""

        tempConfigs[tempConfigId] = config.copy(
            sponsor = sponsor
        )

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleThumbnailModal(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val thumbnailUrl = event.getValue("thumbnail_url")?.asString?.trim() ?: ""

        tempConfigs[tempConfigId] = config.copy(thumbnailUrl = thumbnailUrl)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleRoleWeightModal(event: ModalInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val roleId = idMap["role_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val weightStr = event.getValue("weight")?.asString?.trim() ?: "1"
        val weight = weightStr.toIntOrNull()

        if (weight == null || weight < 1) {
            event.reply("權重必須是大於等於1的整數！填入1代表取消該身份組的權重設定。").setEphemeral(true).queue()
            return
        }

        val newWeights = config.roleWeights.toMutableMap()
        if (weight == 1) {
            newWeights.remove(roleId.toLong())
        } else {
            newWeights[roleId.toLong()] = weight
        }

        tempConfigs[tempConfigId] = config.copy(roleWeights = newWeights)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    /**
     * Parse date time string to Instant
     */
    private fun parseDateTime(dateTimeStr: String): Instant {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val localDateTime = LocalDateTime.parse(dateTimeStr, formatter)
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant()
    }

    // Select menu submission handlers
    private fun handleJoinTimeSelect(event: StringSelectInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val selectedValue = event.values.firstOrNull()?.toLongOrNull()

        tempConfigs[tempConfigId] = config.copy(serverJoinTimeRequirement = selectedValue)

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleEntitySelectRoles(event: EntitySelectInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val selectedRoles = event.values.map { it.idLong }.toSet()

        val updatedConfig = when (config.rolePermissionType) {
            RolePermissionType.PARTIAL_ALLOWED -> config.copy(allowedRoles = selectedRoles)
            RolePermissionType.PARTIAL_DENIED -> config.copy(deniedRoles = selectedRoles)
            else -> config // Should not happen for ALL_ALLOWED
        }

        tempConfigs[tempConfigId] = updatedConfig

        val updatedMessage = createConfigurationEditMessage(tempConfigId, tempConfigs[tempConfigId]!!)
        event.deferEdit().flatMap { _ ->
            event.hook.editOriginal(updatedMessage.build())
        }.queue()
    }

    private fun handleEntitySelectRoleWeights(event: EntitySelectInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val selectedRole = event.values.firstOrNull() ?: return

        val modal = Modal.create(
            componentIdManager.build(
                mapOf(
                    "action" to "modal_role_weight",
                    "giveaway_id" to tempConfigId,
                    "role_id" to selectedRole.id
                )
            ),
            "設定身份組權重"
        ).addComponents(
            ActionRow.of(
                TextInput.create("weight", "權重值", TextInputStyle.SHORT)
                    .setPlaceholder("請輸入權重值 (填入1代表取消設定)")
                    .setValue("1")
                    .setRequired(true)
                    .setMaxLength(3)
                    .build()
            )
        ).build()

        event.replyModal(modal).queue()
    }

    private fun handlePreviewButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val tempConfigId = idMap["giveaway_id"] as String
        val config = tempConfigs[tempConfigId] ?: return

        val previewEmbed = createPreviewEmbed(config)
        event.replyEmbeds(previewEmbed).setEphemeral(true).queue()
    }

    private fun handleCreateButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        // TODO: Implement giveaway creation
        event.reply("建立功能開發中...").setEphemeral(true).queue()
    }

    private fun handleParticipateButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        // TODO: Implement participation logic
        event.reply("參與功能開發中...").setEphemeral(true).queue()
    }

    private fun handleRerollButton(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        // TODO: Implement reroll logic
        event.reply("重新抽取功能開發中...").setEphemeral(true).queue()
    }

    private fun handleRolePermissionSelect(event: StringSelectInteractionEvent, idMap: Map<String, Any>) {
        // TODO: Implement role permission configuration
        event.reply("身份組權限配置開發中...").setEphemeral(true).queue()
    }

    private fun handleRoleWeightsSelect(event: StringSelectInteractionEvent, idMap: Map<String, Any>) {
        // TODO: Implement role weight configuration
        event.reply("身份組權重配置開發中...").setEphemeral(true).queue()
    }
}