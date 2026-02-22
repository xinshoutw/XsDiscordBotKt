package tw.xinshou.discord.plugin.giveaway

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.requests.RestAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.BotLoader.jdaBot
import tw.xinshou.discord.core.builtin.messagecreator.modal.ModalCreator
import tw.xinshou.discord.core.builtin.messagecreator.v2.MessageCreator
import tw.xinshou.discord.core.json.JsonFileManager
import tw.xinshou.discord.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.core.util.ComponentIdManager
import tw.xinshou.discord.core.util.FieldType
import tw.xinshou.discord.plugin.giveaway.Event.componentPrefix
import tw.xinshou.discord.plugin.giveaway.Event.pluginDirectory
import tw.xinshou.discord.plugin.giveaway.create.StepManager
import tw.xinshou.discord.plugin.giveaway.data.*
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal object Giveaway {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private var autoDrawScheduler: ScheduledExecutorService? = null

    val jsonAdapter: JsonAdapter<GiveawayGuildData> = JsonFileManager.moshi.adapterReified<GiveawayGuildData>()
    val jsonGuildManager = JsonGuildFileManager<GiveawayGuildData>(
        dataDirectory = File(pluginDirectory, "data"),
        adapter = jsonAdapter,
        defaultInstance = mutableMapOf()
    )

    val componentIdManager = ComponentIdManager(
        prefix = componentPrefix,
        idKeys = mapOf(
            "action" to FieldType.STRING,
            "sub_action" to FieldType.STRING,
            "giveaway_id" to FieldType.STRING,
        )
    )

    var messageCreator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        componentIdManager = componentIdManager,
    )

    var modalCreator = ModalCreator(
        langDirFile = File(pluginDirectory, "lang"),
        componentIdManager = componentIdManager,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
    )

    internal fun reload() {
        messageCreator = MessageCreator(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            componentIdManager = componentIdManager,
        )

        modalCreator = ModalCreator(
            langDirFile = File(pluginDirectory, "lang"),
            componentIdManager = componentIdManager,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    fun startAutoDrawScheduler(intervalSeconds: Long) {
        stopAutoDrawScheduler()
        val normalizedInterval = intervalSeconds.coerceIn(5, 3600)
        autoDrawScheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "giveaway-auto-draw").apply { isDaemon = true }
        }.also { scheduler ->
            scheduler.scheduleAtFixedRate(
                {
                    runCatching {
                        processAutoDraw()
                    }.onFailure { throwable ->
                        logger.error("Auto draw loop failed: {}", throwable.message, throwable)
                    }
                },
                normalizedInterval,
                normalizedInterval,
                TimeUnit.SECONDS
            )
        }
        logger.info("Giveaway auto draw scheduler started with {}s interval.", normalizedInterval)
    }

    fun stopAutoDrawScheduler() {
        autoDrawScheduler?.shutdownNow()
        autoDrawScheduler = null
        logger.info("Giveaway auto draw scheduler stopped.")
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        StepManager.onSlashCommandInteraction(event)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {
        val idMap = componentIdManager.parse(event.componentId)

        when (idMap["action"]) {
            "create" -> StepManager.onButtonInteraction(event, idMap)
            "participate" -> participate(event, idMap)
            "draw" -> draw(event, idMap, reroll = false)
            "reroll" -> draw(event, idMap, reroll = true)
        }
    }

    fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        logger.debug("Unexpected StringSelect interaction for giveaway: {}", event.componentId)
    }

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        logger.debug("Unexpected EntitySelect interaction for giveaway: {}", event.componentId)
    }

    fun onModalInteraction(event: ModalInteractionEvent) {
        val idMap = componentIdManager.parse(event.modalId)
        if (idMap["action"] == "create") {
            StepManager.onModalInteraction(event, idMap)
        }
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        StepManager.onGuildLeave(event)
        jsonGuildManager.removeAndSave(event.guild.idLong)
    }

    private fun processAutoDraw() {
        val now = Instant.now().epochSecond
        if (jsonGuildManager.mapper.isEmpty()) return

        jsonGuildManager.mapper.values.forEach { manager ->
            var changed = false

            manager.data.values.toList().forEach { giveaway ->
                if (giveaway.ended) return@forEach
                if (giveaway.config.endAtEpochSecond > now) return@forEach

                giveaway.winnerResults.clear()
                giveaway.winnerResults += drawPrizeWinners(giveaway.config, giveaway.participantIds)
                giveaway.ended = true
                giveaway.endedAtEpochSecond = now
                changed = true

                refreshMessage(giveaway)
                announceAutoDrawResult(giveaway)
            }

            if (changed) manager.save()
        }
    }

    fun createGiveaway(
        guildId: Long,
        channel: MessageChannelUnion,
        creatorId: Long,
        config: GiveawayConfig,
        locale: DiscordLocale,
    ): RestAction<Message> {
        val giveawayId = UUID.randomUUID().toString().replace("-", "").take(16)
        val giveaway = GiveawayInstance(
            id = giveawayId,
            guildId = guildId,
            channelId = channel.idLong,
            messageId = 0,
            creatorId = creatorId,
            localeTag = locale.locale,
            config = config.deepCopy(),
        )

        val createData = messageCreator.getCreateBuilder(
            key = "giveaway-post",
            locale = locale,
            modelMapper = giveawayMessageModels(giveaway)
        ).build()

        return channel.sendMessage(createData).map { message ->
            val finalizedGiveaway = giveaway.copy(messageId = message.idLong)
            val manager = jsonGuildManager[guildId]
            manager.data[giveawayId] = finalizedGiveaway
            manager.save()
            message
        }
    }

    private fun participate(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val giveaway = resolveGiveaway(event, idMap) ?: return

        if (event.messageIdLong != giveaway.messageId) {
            event.deferEdit().queue()
            return
        }

        val locale = localeOf(giveaway)
        val isZh = isZh(locale)

        if (giveaway.ended || Instant.now().epochSecond >= giveaway.config.endAtEpochSecond) {
            event.reply(
                if (isZh) "抽獎已截止，無法再參加。" else "This giveaway is closed."
            ).setEphemeral(true).queue()
            return
        }

        val joined = giveaway.participantIds.add(event.user.idLong)
        if (!joined) {
            giveaway.participantIds.remove(event.user.idLong)
        }

        saveGiveaway(giveaway)
        refreshMessage(event, giveaway)

        event.reply(
            if (joined) {
                if (isZh) "你已成功參加抽獎。" else "You joined the giveaway."
            } else {
                if (isZh) "你已取消參加抽獎。" else "You left the giveaway."
            }
        ).setEphemeral(true).queue()
    }

    private fun draw(event: ButtonInteractionEvent, idMap: Map<String, Any>, reroll: Boolean) {
        val giveaway = resolveGiveaway(event, idMap) ?: return
        val locale = localeOf(giveaway)
        val isZh = isZh(locale)

        val member = event.member
        if (member == null || (member.idLong != giveaway.creatorId && !member.hasPermission(Permission.ADMINISTRATOR))) {
            event.reply(
                if (isZh) "只有建立者或管理員可執行此操作。" else "Only creator/admin can do this."
            ).setEphemeral(true).queue()
            return
        }

        val now = Instant.now().epochSecond
        if (!reroll) {
            if (giveaway.ended) {
                event.reply(if (isZh) "此抽獎已開獎，可使用重抽。" else "Already drawn. Use reroll.")
                    .setEphemeral(true)
                    .queue()
                return
            }

            if (now < giveaway.config.endAtEpochSecond) {
                event.reply(
                    if (isZh) {
                        "尚未到開獎時間（<t:${giveaway.config.endAtEpochSecond}:R>）。"
                    } else {
                        "Too early to draw winners (<t:${giveaway.config.endAtEpochSecond}:R>)."
                    }
                ).setEphemeral(true).queue()
                return
            }
        } else if (!giveaway.ended) {
            event.reply(if (isZh) "抽獎尚未開獎，不能重抽。" else "Giveaway not drawn yet.")
                .setEphemeral(true)
                .queue()
            return
        }

        giveaway.winnerResults.clear()
        giveaway.winnerResults += drawPrizeWinners(giveaway.config, giveaway.participantIds)
        giveaway.ended = true
        giveaway.endedAtEpochSecond = now

        saveGiveaway(giveaway)
        refreshMessage(event, giveaway)

        val (header, body) = buildDrawResultContent(giveaway, locale, reroll)
        event.reply(
            messageCreator.getCreateBuilder(
                key = "draw-result",
                locale = locale,
                replaceMap = mapOf(
                    "ga@header" to header,
                    "ga@body" to body,
                    "ga@title" to giveaway.config.title,
                )
            ).build()
        ).setEphemeral(true).queue()
    }

    private fun buildDrawResultContent(
        giveaway: GiveawayInstance,
        locale: DiscordLocale,
        reroll: Boolean
    ): Pair<String, String> {
        val isZh = isZh(locale)

        val header = if (isZh) {
            if (reroll) "重抽完成" else "開獎完成"
        } else {
            if (reroll) "Reroll finished" else "Draw finished"
        }

        if (giveaway.participantIds.isEmpty()) {
            val body = if (isZh) {
                "目前沒有任何參與者。"
            } else {
                "There are no participants."
            }
            return header to body
        }

        val lines = giveaway.winnerResults.map { result ->
            val winners = if (result.winnerIds.isEmpty()) {
                if (isZh) "（名額不足）" else "(not enough participants)"
            } else {
                result.winnerIds.joinToString(" ") { "<@${it}>" }
            }
            "${result.prizeName}: $winners"
        }

        return header to clipFieldText(lines.joinToString("\n"), 1800)
    }

    private fun resolveGiveaway(event: ButtonInteractionEvent, idMap: Map<String, Any>): GiveawayInstance? {
        val guild = event.guild ?: run {
            event.reply("Guild not found.").setEphemeral(true).queue()
            return null
        }

        val giveawayId = idMap["giveaway_id"] as? String ?: run {
            event.reply("Invalid giveaway id.").setEphemeral(true).queue()
            return null
        }

        val giveaway = jsonGuildManager[guild.idLong].data[giveawayId]
        if (giveaway == null) {
            val isZh = event.userLocale.locale.startsWith("zh")
            event.reply(
                if (isZh) "找不到該抽獎，可能已被刪除。" else "Giveaway not found."
            ).setEphemeral(true).queue()
            return null
        }

        return giveaway
    }

    private fun saveGiveaway(giveaway: GiveawayInstance) {
        val manager = jsonGuildManager[giveaway.guildId]
        manager.data[giveaway.id] = giveaway
        manager.save()
    }

    private fun refreshMessage(event: ButtonInteractionEvent, giveaway: GiveawayInstance) {
        val locale = localeOf(giveaway)
        event.message.editMessage(
            messageCreator.getEditBuilder(
                key = "giveaway-post",
                locale = locale,
                modelMapper = giveawayMessageModels(giveaway)
            ).build()
        ).queue({}, {
            logger.warn("Failed to refresh giveaway message: {}", it.message)
        })
    }

    private fun refreshMessage(giveaway: GiveawayInstance) {
        val guild = jdaBot.getGuildById(giveaway.guildId) ?: return
        val channel = guild.getTextChannelById(giveaway.channelId) ?: return
        val locale = localeOf(giveaway)

        channel.retrieveMessageById(giveaway.messageId).queue({ message ->
            message.editMessage(
                messageCreator.getEditBuilder(
                    key = "giveaway-post",
                    locale = locale,
                    modelMapper = giveawayMessageModels(giveaway)
                ).build()
            ).queue({}, {
                logger.warn("Failed to edit giveaway message {}: {}", giveaway.messageId, it.message)
            })
        }, {
            logger.warn("Failed to retrieve giveaway message {}: {}", giveaway.messageId, it.message)
        })
    }

    private fun announceAutoDrawResult(giveaway: GiveawayInstance) {
        val guild = jdaBot.getGuildById(giveaway.guildId) ?: return
        val channel = guild.getTextChannelById(giveaway.channelId) ?: return
        val locale = localeOf(giveaway)
        val (header, body) = buildDrawResultContent(giveaway, locale, reroll = false)

        channel.sendMessage(
            messageCreator.getCreateBuilder(
                key = "auto-draw-result",
                locale = locale,
                replaceMap = mapOf(
                    "ga@header" to header,
                    "ga@body" to body,
                    "ga@title" to giveaway.config.title,
                )
            ).build()
        ).queue({}, {
            logger.warn("Failed to announce auto draw for {}: {}", giveaway.id, it.message)
        })
    }

    private fun giveawayMessageModels(giveaway: GiveawayInstance): Map<String, Any> {
        val locale = localeOf(giveaway)
        return mapOf(
            "ga@giveaway-embed" to giveawayEmbed(giveaway, locale),
            "ga@join-button" to joinButton(giveaway, locale),
            "ga@draw-button" to drawButton(giveaway, locale),
            "ga@reroll-button" to rerollButton(giveaway, locale),
        )
    }

    private fun giveawayEmbed(giveaway: GiveawayInstance, locale: DiscordLocale): MessageEmbed {
        val isZh = isZh(locale)
        val now = Instant.now().epochSecond

        val status = when {
            giveaway.ended -> if (isZh) "已開獎" else "Drawn"
            now >= giveaway.config.endAtEpochSecond -> if (isZh) "可開獎" else "Ready to draw"
            else -> if (isZh) "進行中" else "Running"
        }

        val prizeLines = giveaway.config.prizes.mapIndexed { index, prize ->
            val suffix = if (isZh) "位" else "winner(s)"
            "${index + 1}. ${prize.name} (${prize.winnerCount} $suffix)"
        }.joinToString("\n")

        val winnerPolicy = when (giveaway.config.winnerDuplicatePolicy) {
            WinnerDuplicatePolicy.ALLOW_DUPLICATE -> {
                if (isZh) "每個獎品可重複中獎" else "Duplicates allowed across prizes"
            }

            WinnerDuplicatePolicy.UNIQUE_ACROSS_PRIZES -> {
                if (isZh) "跨獎品不得重複中獎" else "Unique winners across prizes"
            }
        }

        val winnerLines = if (!giveaway.ended) {
            if (isZh) "尚未開獎" else "Not drawn yet"
        } else {
            giveaway.winnerResults.joinToString("\n") { result ->
                val mentions = if (result.winnerIds.isEmpty()) {
                    if (isZh) "（名額不足）" else "(not enough participants)"
                } else {
                    result.winnerIds.joinToString(" ") { "<@${it}>" }
                }
                "${result.prizeName}: $mentions"
            }
        }

        return EmbedBuilder()
            .setTitle("🎉 ${giveaway.config.title}")
            .setDescription(
                giveaway.config.description.ifBlank {
                    if (isZh) "按下下方按鈕參加抽獎。" else "Press the button below to join."
                }
            )
            .setColor(if (giveaway.ended) 0x95A5A6 else 0xF1C40F)
            .addField(
                if (isZh) "狀態" else "Status",
                "$status\n<t:${giveaway.config.endAtEpochSecond}:R>",
                true
            )
            .addField(
                if (isZh) "參與人數" else "Participants",
                giveaway.participantIds.size.toString(),
                true,
            )
            .addField(
                if (isZh) "中獎限制" else "Winner Policy",
                winnerPolicy,
                false,
            )
            .addField(
                if (isZh) "獎品" else "Prizes",
                clipFieldText(prizeLines),
                false,
            )
            .addField(
                if (isZh) "中獎結果" else "Winner Results",
                clipFieldText(winnerLines),
                false,
            )
            .apply {
                if (giveaway.config.sponsor.isNotBlank()) {
                    addField(
                        if (isZh) "主辦 / 贊助" else "Sponsor",
                        giveaway.config.sponsor,
                        true,
                    )
                }

                if (giveaway.config.thumbnailUrl.startsWith("http://") || giveaway.config.thumbnailUrl.startsWith("https://")) {
                    setThumbnail(giveaway.config.thumbnailUrl)
                }
            }
            .setFooter(
                if (isZh) {
                    "Giveaway ID: ${giveaway.id}"
                } else {
                    "Giveaway ID: ${giveaway.id}"
                }
            )
            .build()
    }

    private fun joinButton(giveaway: GiveawayInstance, locale: DiscordLocale): Button {
        val isZh = isZh(locale)
        val closed = giveaway.ended || Instant.now().epochSecond >= giveaway.config.endAtEpochSecond

        return Button.of(
            ButtonStyle.SUCCESS,
            componentIdManager.build(
                mapOf(
                    "action" to "participate",
                    "giveaway_id" to giveaway.id,
                )
            ),
            if (isZh) "參加 / 退出" else "Join / Leave"
        ).withDisabled(closed)
    }

    private fun drawButton(giveaway: GiveawayInstance, locale: DiscordLocale): Button {
        val isZh = isZh(locale)

        return Button.of(
            ButtonStyle.PRIMARY,
            componentIdManager.build(
                mapOf(
                    "action" to "draw",
                    "giveaway_id" to giveaway.id,
                )
            ),
            if (isZh) "手動開獎" else "Draw Winners"
        ).withDisabled(giveaway.ended)
    }

    private fun rerollButton(giveaway: GiveawayInstance, locale: DiscordLocale): Button {
        val isZh = isZh(locale)

        return Button.of(
            ButtonStyle.SECONDARY,
            componentIdManager.build(
                mapOf(
                    "action" to "reroll",
                    "giveaway_id" to giveaway.id,
                )
            ),
            if (isZh) "重抽" else "Reroll"
        ).withDisabled(!giveaway.ended)
    }

    private fun localeOf(giveaway: GiveawayInstance): DiscordLocale {
        return runCatching { DiscordLocale.from(giveaway.localeTag) }
            .getOrDefault(DiscordLocale.CHINESE_TAIWAN)
    }

    private fun isZh(locale: DiscordLocale): Boolean = locale.locale.startsWith("zh")

    private fun clipFieldText(value: String, max: Int = 1024): String {
        if (value.length <= max) return value
        return value.take(max - 3) + "..."
    }
}
