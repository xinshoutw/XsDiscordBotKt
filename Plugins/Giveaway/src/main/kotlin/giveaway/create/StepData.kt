package tw.xinshou.discord.plugin.giveaway.create

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import tw.xinshou.discord.plugin.giveaway.Giveaway.messageCreator
import tw.xinshou.discord.plugin.giveaway.data.GiveawayConfig
import tw.xinshou.discord.plugin.giveaway.data.WinnerDuplicatePolicy
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class Step(
    val hook: InteractionHook,
    val guildId: Long,
) {
    val data: GiveawayConfig = GiveawayConfig()

    fun renderEmbedAction(locale: DiscordLocale): WebhookMessageEditAction<Message?> {
        val messageEditData = messageCreator.getEditBuilder(
            key = "create-giveaway",
            locale = locale,
            modelMapper = mapOf(
                "ga@preview-embed" to previewEmbed(locale),
            )
        ).build()

        return hook.editOriginal(messageEditData)
    }

    fun toggleWinnerDuplicatePolicy() {
        data.winnerDuplicatePolicy = when (data.winnerDuplicatePolicy) {
            WinnerDuplicatePolicy.ALLOW_DUPLICATE -> WinnerDuplicatePolicy.UNIQUE_ACROSS_PRIZES
            WinnerDuplicatePolicy.UNIQUE_ACROSS_PRIZES -> WinnerDuplicatePolicy.ALLOW_DUPLICATE
        }
    }

    fun removeLastPrize(): Boolean {
        if (data.prizes.isEmpty()) return false
        data.prizes.removeAt(data.prizes.lastIndex)
        return true
    }

    fun validate(locale: DiscordLocale, nowEpochSecond: Long = Instant.now().epochSecond): String? {
        val isZh = locale.locale.startsWith("zh")
        if (data.title.isBlank()) {
            return if (isZh) "請先設定抽獎名稱。" else "Please set giveaway title first."
        }
        if (data.endAtEpochSecond <= nowEpochSecond) {
            return if (isZh) "請設定未來的結束時間。" else "Please set an end time in the future."
        }
        if (data.prizes.isEmpty()) {
            return if (isZh) "請至少新增一個獎品。" else "Please add at least one prize."
        }
        return null
    }

    private fun previewEmbed(locale: DiscordLocale): MessageEmbed {
        val isZh = locale.locale.startsWith("zh")
        val title = if (data.title.isBlank()) {
            if (isZh) "🎁 抽獎建立預覽" else "🎁 Giveaway Draft"
        } else {
            "🎁 ${data.title}"
        }

        val prizeLines = if (data.prizes.isEmpty()) {
            if (isZh) "尚未新增獎品" else "No prize yet"
        } else {
            data.prizes.mapIndexed { index, prize ->
                val winnerWord = if (isZh) "位" else "winner(s)"
                "${index + 1}. ${prize.name} (${prize.winnerCount} $winnerWord)"
            }.joinToString("\n")
        }

        val endTimeText = if (data.endAtEpochSecond <= 0) {
            if (isZh) "未設定" else "Not set"
        } else {
            val absolute = formatDateTime(data.endAtEpochSecond)
            "<t:${data.endAtEpochSecond}:F>\n$absolute"
        }

        val duplicateText = when (data.winnerDuplicatePolicy) {
            WinnerDuplicatePolicy.ALLOW_DUPLICATE -> if (isZh) "允許跨獎品重複中獎" else "Duplicate winners allowed"
            WinnerDuplicatePolicy.UNIQUE_ACROSS_PRIZES -> if (isZh) "跨獎品不可重複中獎" else "Unique winners across prizes"
        }

        val sponsorText = data.sponsor.ifBlank {
            if (isZh) "未設定" else "Not set"
        }

        val descriptionText = data.description.ifBlank {
            if (isZh) "未設定" else "Not set"
        }

        return EmbedBuilder()
            .setTitle(title)
            .setColor(0x2ECC71)
            .addField(if (isZh) "抽獎說明" else "Description", descriptionText, false)
            .addField(if (isZh) "結束時間" else "End Time", endTimeText, false)
            .addField(if (isZh) "獎品清單" else "Prizes", prizeLines, false)
            .addField(
                if (isZh) "中獎限制" else "Winner Policy",
                duplicateText,
                false,
            )
            .addField(
                if (isZh) "總中獎名額" else "Total Winner Slots",
                data.totalWinnerSlots().toString(),
                true,
            )
            .addField(if (isZh) "主辦/贊助" else "Sponsor", sponsorText, true)
            .apply {
                if (data.thumbnailUrl.startsWith("http://") || data.thumbnailUrl.startsWith("https://")) {
                    setThumbnail(data.thumbnailUrl)
                }
            }
            .setFooter(
                if (isZh) {
                    "使用下方按鈕逐步設定，最後按「確認建立」"
                } else {
                    "Configure step by step, then click Confirm Create"
                }
            )
            .build()
    }

    companion object {
        private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        fun formatDateTime(epochSecond: Long): String {
            return Instant.ofEpochSecond(epochSecond)
                .atZone(ZoneId.systemDefault())
                .format(dateTimeFormatter)
        }

        fun parseDateTime(dateTime: String): Long {
            val instant = java.time.LocalDateTime
                .parse(dateTime, dateTimeFormatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
            return instant.epochSecond
        }
    }
}
