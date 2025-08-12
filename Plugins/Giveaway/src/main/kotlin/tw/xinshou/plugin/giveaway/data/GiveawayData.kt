package tw.xinshou.plugin.giveaway.data

import java.time.Instant

/**
 * Represents the configuration for a giveaway
 */
data class GiveawayConfig(
    val giveawayName: String = "",
    val prizeName: String = "",
    val winnerCount: Int = 1,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val duration: Long? = null, // in seconds
    val rolePermissionType: RolePermissionType = RolePermissionType.ALL_ALLOWED,
    val allowedRoles: Set<Long> = emptySet(),
    val deniedRoles: Set<Long> = emptySet(),
    val roleWeights: Map<Long, Int> = emptyMap(),
    val isWeightAdditive: Boolean = false,
    val sponsor: String = "",
    val minAccountAge: Instant? = null, // minimum account creation time
    val shouldDmWinners: Boolean = true,
    val serverJoinTimeRequirement: Long? = null, // in seconds, null means no requirement
    val thumbnailUrl: String = "" // URL for giveaway thumbnail image
) {
    /**
     * Calculate the actual end time based on start time and duration
     * Priority: endTime > startTime + duration > current time + duration
     */
    fun getCalculatedEndTime(): Instant {
        return when {
            endTime != null -> endTime
            startTime != null && duration != null -> startTime.plusSeconds(duration)
            duration != null -> Instant.now().plusSeconds(duration)
            else -> Instant.now().plusSeconds(3600) // default 1 hour
        }
    }

    /**
     * Calculate the actual start time
     * Priority: startTime > current time
     */
    fun getCalculatedStartTime(): Instant {
        return startTime ?: Instant.now()
    }
}

/**
 * Represents the type of role permission system
 */
enum class RolePermissionType {
    ALL_ALLOWED,           // Everyone can participate
    PARTIAL_ALLOWED,       // Only specified roles can participate (default deny)
    PARTIAL_DENIED         // Everyone except specified roles can participate (default allow)
}

/**
 * Represents an active giveaway instance
 */
data class GiveawayInstance(
    val id: String,
    val config: GiveawayConfig,
    val messageId: Long,
    val channelId: Long,
    val guildId: Long,
    val creatorId: Long,
    val participants: MutableSet<Long> = mutableSetOf(),
    val createdAt: Instant = Instant.now(),
    val isActive: Boolean = true,
    val winners: List<Long> = emptyList()
)

/**
 * Represents a participant in a giveaway with their calculated weight
 */
data class GiveawayParticipant(
    val userId: Long,
    val weight: Int = 1
)