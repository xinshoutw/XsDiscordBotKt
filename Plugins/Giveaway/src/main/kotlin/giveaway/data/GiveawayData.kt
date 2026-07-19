package tw.xinshou.discord.plugin.giveaway.data

import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.math.min
import kotlin.random.Random

@Serializable
data class GiveawayPrize(
    val name: String,
    val winnerCount: Int = 1,
)

enum class WinnerDuplicatePolicy {
    ALLOW_DUPLICATE,
    UNIQUE_ACROSS_PRIZES,
}

@Serializable
data class GiveawayConfig(
    var title: String = "",
    var description: String = "",
    var sponsor: String = "",
    var thumbnailUrl: String = "",
    var endAtEpochSecond: Long = 0,
    var winnerDuplicatePolicy: WinnerDuplicatePolicy = WinnerDuplicatePolicy.ALLOW_DUPLICATE,
    val prizes: MutableList<GiveawayPrize> = mutableListOf(),
) {
    fun isReady(nowEpochSecond: Long = Instant.now().epochSecond): Boolean {
        return title.isNotBlank() && endAtEpochSecond > nowEpochSecond && prizes.isNotEmpty()
    }

    fun deepCopy(): GiveawayConfig {
        return GiveawayConfig(
            title = title, description = description, sponsor = sponsor,
            thumbnailUrl = thumbnailUrl, endAtEpochSecond = endAtEpochSecond,
            winnerDuplicatePolicy = winnerDuplicatePolicy,
            prizes = prizes.map { it.copy() }.toMutableList(),
        )
    }

    fun totalWinnerSlots(): Int = prizes.sumOf { it.winnerCount.coerceAtLeast(1) }
}

@Serializable
data class PrizeWinners(
    val prizeName: String,
    val winnerIds: MutableList<Long> = mutableListOf(),
)

@Serializable
data class GiveawayInstance(
    val id: String,
    val guildId: Long,
    val channelId: Long,
    val messageId: Long,
    val creatorId: Long,
    val localeTag: String = "zh-TW",
    val createdAtEpochSecond: Long = Instant.now().epochSecond,
    var endedAtEpochSecond: Long? = null,
    var ended: Boolean = false,
    val config: GiveawayConfig,
    val participantIds: MutableSet<Long> = mutableSetOf(),
    val winnerResults: MutableList<PrizeWinners> = mutableListOf(),
)

internal typealias GiveawayGuildData = MutableMap<String, GiveawayInstance>

internal fun drawPrizeWinners(
    config: GiveawayConfig, participants: Set<Long>, random: Random = Random,
): MutableList<PrizeWinners> {
    if (config.prizes.isEmpty()) return mutableListOf()

    val basePool = participants.toMutableList()
    val uniquePool = participants.toMutableList()
    val results = mutableListOf<PrizeWinners>()

    for (prize in config.prizes) {
        val targetCount = prize.winnerCount.coerceAtLeast(1)
        val pool = when (config.winnerDuplicatePolicy) {
            WinnerDuplicatePolicy.ALLOW_DUPLICATE -> basePool
            WinnerDuplicatePolicy.UNIQUE_ACROSS_PRIZES -> uniquePool
        }

        if (pool.isEmpty()) {
            results += PrizeWinners(prize.name, mutableListOf())
            continue
        }

        val shuffled = pool.shuffled(random)
        val picked = shuffled.take(min(targetCount, shuffled.size))
        results += PrizeWinners(prize.name, picked.toMutableList())

        if (config.winnerDuplicatePolicy == WinnerDuplicatePolicy.UNIQUE_ACROSS_PRIZES) {
            uniquePool.removeAll(picked.toSet())
        }
    }

    return results
}
