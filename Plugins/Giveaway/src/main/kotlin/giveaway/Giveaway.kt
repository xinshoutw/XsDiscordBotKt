package tw.xinshou.discord.plugin.giveaway

import core.i18n.MessageTemplate
import core.util.ComponentId
import core.util.GuildJsonFile
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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
import tw.xinshou.discord.plugin.giveaway.Event.config
import tw.xinshou.discord.plugin.giveaway.Event.pluginDirectory
import tw.xinshou.discord.plugin.giveaway.create.StepManager
import tw.xinshou.discord.plugin.giveaway.data.GiveawayConfig
import tw.xinshou.discord.plugin.giveaway.data.GiveawayGuildData
import tw.xinshou.discord.plugin.giveaway.data.GiveawayInstance
import tw.xinshou.discord.plugin.giveaway.data.WinnerDuplicatePolicy
import tw.xinshou.discord.plugin.giveaway.data.drawPrizeWinners
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal object Giveaway {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private var autoDrawScheduler: ScheduledExecutorService? = null
    private var defaultLocale: DiscordLocale = DiscordLocale.CHINESE_TAIWAN

    val jsonGuildManager = GuildJsonFile(
        directory = File(pluginDirectory, "data"),
        serializer = MapSerializer(String.serializer(), GiveawayInstance.serializer()),
        defaultInstance = { mutableMapOf() },
    )

    val componentId = ComponentId(
        prefix = config.componentPrefix,
        idKeys = mapOf(
            "action" to ComponentId.FieldType.STRING,
            "sub_action" to ComponentId.FieldType.STRING,
            "giveaway_id" to ComponentId.FieldType.STRING,
        )
    )

    var messageTemplate = MessageTemplate(
        langDir = File(pluginDirectory, "lang"),
        defaultLocale = defaultLocale,
        componentIdPrefix = config.componentPrefix,
    )

    internal fun reload(defaultLocale: DiscordLocale = this.defaultLocale) {
        this.defaultLocale = defaultLocale
        messageTemplate = MessageTemplate(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = this.defaultLocale,
            componentIdPrefix = config.componentPrefix,
        )
    }

    fun startAutoDrawScheduler(intervalSeconds: Long) {
        stopAutoDrawScheduler()
        val normalizedInterval = intervalSeconds.coerceIn(5, 3600)
        autoDrawScheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "giveaway-auto-draw").apply { isDaemon = true }
        }.also { scheduler ->
            scheduler.scheduleAtFixedRate({
                runCatching { processAutoDraw() }.onFailure { throwable ->
                    logger.error("Auto draw loop failed: {}", throwable.message, throwable)
                }
            }, 5, normalizedInterval, TimeUnit.SECONDS)
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
        val idMap = componentId.parse(event.componentId)
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
        val idMap = componentId.parse(event.modalId)
        if (idMap["action"] == "create") {
            StepManager.onModalInteraction(event, idMap)
        }
    }

    fun onGuildLeave(event: GuildLeaveEvent) {
        StepManager.onGuildLeave(event)
        jsonGuildManager[event.guild.idLong].delete()
    }

    private fun processAutoDraw() {
        val now = Instant.now().epochSecond
        val dataDir = File(pluginDirectory, "data")
        if (!dataDir.exists()) return

        dataDir.listFiles()?.filter { it.isFile && it.extension == "json" }?.forEach { file ->
            val guildId = file.nameWithoutExtension.toLongOrNull() ?: return@forEach
            val manager = jsonGuildManager[guildId]
            var changed = false

            manager.data.values.toList().forEach { giveaway ->
                if (giveaway.ended) return@forEach
                if (giveaway.config.endAtEpochSecond > now) return@forEach

                giveaway.winnerResults.clear()
                giveaway.winnerResults += drawPrizeWinners(giveaway.config, giveaway.participantIds)
                giveaway.ended = true
                giveaway.endedAtEpochSecond = now
                changed = true
            }

            if (changed) manager.save()
        }
    }

    fun createGiveaway(
        guildId: Long, channel: MessageChannelUnion, creatorId: Long,
        config: GiveawayConfig, locale: DiscordLocale,
    ): RestAction<Message> {
        val manager = jsonGuildManager[guildId]
        var giveawayId = ""
        do {
            giveawayId = UUID.randomUUID().toString().replace("-", "").take(16)
        } while (manager.data.containsKey(giveawayId))

        val giveaway = GiveawayInstance(
            id = giveawayId, guildId = guildId, channelId = channel.idLong,
            messageId = 0, creatorId = creatorId, localeTag = locale.locale,
            config = config.deepCopy(),
        )

        val createData = messageTemplate.buildCreate(
            messageId = "giveaway-post", locale = locale,
        ).build()

        return channel.sendMessage(createData).map { message ->
            val finalizedGiveaway = giveaway.copy(messageId = message.idLong)
            @Suppress("UNCHECKED_CAST")
            (manager.data as MutableMap<String, GiveawayInstance>)[giveawayId] = finalizedGiveaway
            manager.save()
            message
        }
    }

    private fun participate(event: ButtonInteractionEvent, idMap: Map<String, Any>) {
        val giveaway = resolveGiveaway(event, idMap) ?: return
        val locale = localeOf(giveaway)
        val isZh = isZh(locale)

        if (giveaway.ended || Instant.now().epochSecond >= giveaway.config.endAtEpochSecond) {
            event.reply(if (isZh) "抽獎已截止，無法再參加。" else "This giveaway is closed.").setEphemeral(true).queue()
            return
        }

        val wasAdded = giveaway.participantIds.add(event.user.idLong)
        if (!wasAdded) giveaway.participantIds.remove(event.user.idLong)

        saveGiveaway(giveaway)

        event.reply(
            if (wasAdded) { if (isZh) "你已成功參加抽獎。" else "You joined the giveaway." }
            else { if (isZh) "你已取消參加抽獎。" else "You left the giveaway." }
        ).setEphemeral(true).queue()
    }

    private fun draw(event: ButtonInteractionEvent, idMap: Map<String, Any>, reroll: Boolean) {
        val giveaway = resolveGiveaway(event, idMap) ?: return
        val locale = localeOf(giveaway)
        val isZh = isZh(locale)

        val member = event.member
        if (member == null) {
            event.reply(if (isZh) "只有建立者或管理員可執行此操作。" else "Only creator/admin can do this.").setEphemeral(true).queue()
            return
        }

        val isAdmin = member.hasPermission(Permission.ADMINISTRATOR)
        val isCreator = member.idLong == giveaway.creatorId
        if (!((!reroll && (isCreator || isAdmin)) || (reroll && isAdmin))) {
            event.reply(if (isZh) "只有建立者或管理員可執行此操作。" else "Only creator/admin can do this.").setEphemeral(true).queue()
            return
        }

        val now = Instant.now().epochSecond
        if (!reroll && giveaway.ended) {
            event.reply(if (isZh) "此抽獎已開獎，可使用重抽。" else "Already drawn. Use reroll.").setEphemeral(true).queue()
            return
        }
        if (!reroll && now < giveaway.config.endAtEpochSecond) {
            event.reply(if (isZh) "尚未到開獎時間。" else "Too early to draw winners.").setEphemeral(true).queue()
            return
        }
        if (reroll && !giveaway.ended) {
            event.reply(if (isZh) "抽獎尚未開獎，不能重抽。" else "Giveaway not drawn yet.").setEphemeral(true).queue()
            return
        }

        giveaway.winnerResults.clear()
        giveaway.winnerResults += drawPrizeWinners(giveaway.config, giveaway.participantIds)
        giveaway.ended = true
        giveaway.endedAtEpochSecond = now

        saveGiveaway(giveaway)
        event.deferEdit().queue()
    }

    private fun resolveGiveaway(event: ButtonInteractionEvent, idMap: Map<String, Any>): GiveawayInstance? {
        val guild = event.guild ?: run {
            event.reply("Guild not found.").setEphemeral(true).queue(); return null
        }
        val giveawayId = idMap["giveaway_id"] as? String ?: run {
            event.reply("Invalid giveaway id.").setEphemeral(true).queue(); return null
        }
        return jsonGuildManager[guild.idLong].data[giveawayId] ?: run {
            event.reply("Giveaway not found.").setEphemeral(true).queue(); null
        }
    }

    private fun saveGiveaway(giveaway: GiveawayInstance) {
        val manager = jsonGuildManager[giveaway.guildId]
        @Suppress("UNCHECKED_CAST")
        (manager.data as MutableMap<String, GiveawayInstance>)[giveaway.id] = giveaway
        manager.save()
    }

    private fun localeOf(giveaway: GiveawayInstance): DiscordLocale =
        runCatching { DiscordLocale.from(giveaway.localeTag) }.getOrDefault(defaultLocale)

    private fun isZh(locale: DiscordLocale): Boolean =
        locale.locale.substringBefore('-').equals("zh", ignoreCase = true)

    private fun clipFieldText(value: String, max: Int = 1024): String {
        if (value.length <= max) return value
        return value.take(max - 3) + "..."
    }
}
