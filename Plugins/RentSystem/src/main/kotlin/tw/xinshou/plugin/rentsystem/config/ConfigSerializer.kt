package tw.xinshou.plugin.rentsystem.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,

    @SerialName("global_messages")
    val globalMessages: GlobalMessages,

    @SerialName("guild_id")
    val guildId: Long,

    @SerialName("forum_channel_id")
    val forumChannelId: Long,

    @SerialName("time_range")
    val timeRange: TimeRange,

    val schedule: Schedule,
    val members: List<Member>,

    @SerialName("expense_categories")
    val expenseCategories: Map<String, ExpenseCategory>
) {
    @Serializable
    data class GlobalMessages(
        @SerialName("time_not_yet_title")
        val timeNotYetTitle: String,

        @SerialName("forum_channel_name_format")
        val forumChannelNameFormat: String,

        @SerialName("rent_reminder_message")
        val rentReminderMessage: String,

        @SerialName("water_power_reminder_message")
        val waterPowerReminderMessage: String
    )

    @Serializable
    data class TimeRange(
        @SerialName("start_year")
        val startYear: Int,

        @SerialName("start_month")
        val startMonth: Int,

        @SerialName("end_year")
        val endYear: Int,

        @SerialName("end_month")
        val endMonth: Int
    )

    @Serializable
    data class Schedule(
        @SerialName("rent_reminder_day")
        val rentReminderDay: Int,

        @SerialName("rent_reminder_hour")
        val rentReminderHour: Int,

        @SerialName("rent_reminder_minute")
        val rentReminderMinute: Int,

        @SerialName("utility_reminder_day")
        val utilityReminderDay: Int,

        @SerialName("utility_reminder_hour")
        val utilityReminderHour: Int,

        @SerialName("utility_reminder_minute")
        val utilityReminderMinute: Int
    )

    @Serializable
    data class Member(
        @SerialName("room_id")
        val roomId: String,

        @SerialName("room_owner_name")
        val roomOwnerName: String,

        @SerialName("room_owner_discord_id")
        val roomOwnerDiscordId: Long,

        @SerialName("monthly_rent")
        val monthlyRent: Long = 0
    )

    @Serializable
    data class ExpenseCategory(
        val enabled: Boolean,

        @SerialName("item_type_name")
        val itemTypeName: String,

        @SerialName("calculation_method")
        val calculationMethod: String,

        @SerialName("requires_usage_data")
        val requiresUsageData: Boolean,

        @SerialName("fixed_cost")
        val fixedCost: Long? = null,

        val meters: List<Meter>? = null,

        @SerialName("share_members")
        val shareMembers: List<Long>
    )

    @Serializable
    data class Meter(
        @SerialName("meter_id")
        val meterId: String,

        @SerialName("meter_name")
        val meterName: String,

        @SerialName("meter_type")
        val meterType: String,

        @SerialName("owner_id")
        val ownerId: Long? = null
    )
}
