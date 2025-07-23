package tw.xinshou.plugin.rentsystem.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MainConfigSerializer(
    @SerialName("guild_id")
    val guildId: Long,

    @SerialName("forum_channel_id")
    val forumChannelId: Long,

    @SerialName("forum_channel_name_format")
    val forumChannelNameFormat: String,

    val time: TimeConfig,
    val members: List<Member>,

    @SerialName("expense_categories")
    val expenseCategories: Map<String, CategoryConfig>
) {
    @Serializable
    data class TimeConfig(
        @SerialName("start_time")
        val startTime: TimeDetail,

        @SerialName("end_time")
        val endTime: TimeDetail
    ) {
        @Serializable
        data class TimeDetail(
            val year: Int,
            val month: Int
        )
    }

    @Serializable
    data class Member(
        val room: String,
        val name: String,
        val id: Long,
        val rent: Int = 0
    )

    @Serializable
    data class CategoryConfig(
        val enabled: Boolean,

        @SerialName("calculation_method")
        val calculationMethod: String,

        @SerialName("fixed_cost")
        val fixedCost: Double? = null,
        val meters: List<String>? = null,
        val share: List<Long>
    )
}
