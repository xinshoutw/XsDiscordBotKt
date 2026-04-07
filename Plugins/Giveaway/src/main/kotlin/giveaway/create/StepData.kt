package tw.xinshou.discord.plugin.giveaway.create

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import tw.xinshou.discord.plugin.giveaway.Giveaway.messageTemplate
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
        val messageEditData = messageTemplate.buildEdit(
            messageId = "create-giveaway",
            locale = locale,
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
        if (data.title.isBlank()) return if (isZh) "請先設定抽獎名稱。" else "Please set giveaway title first."
        if (data.endAtEpochSecond <= nowEpochSecond) return if (isZh) "請設定未來的結束時間。" else "Please set an end time in the future."
        if (data.prizes.isEmpty()) return if (isZh) "請至少新增一個獎品。" else "Please add at least one prize."
        return null
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
