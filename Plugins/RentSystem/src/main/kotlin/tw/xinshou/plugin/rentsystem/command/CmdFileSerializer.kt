package tw.xinshou.plugin.rentsystem.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.loader.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("daily-electricity")
    val dailyElectricity: CommandDailyElectricity,
    @SerialName("electricity-bill")
    val electricityBill: CommandElectricityBill,
    @SerialName("water-bill")
    val waterBill: CommandWaterBill,
    @SerialName("rent-overview")
    val rentOverview: CommandRentOverview,
) {
    @Serializable
    internal data class CommandDailyElectricity(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val public: LocalTemplate.NameDescriptionString,
            @SerialName("room-a")
            val roomA: LocalTemplate.NameDescriptionString,
            @SerialName("room-b")
            val roomB: LocalTemplate.NameDescriptionString,
            @SerialName("room-c")
            val roomC: LocalTemplate.NameDescriptionString,
            val date: LocalTemplate.NameDescriptionString,
        )
    }

    @Serializable
    internal data class CommandElectricityBill(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            @SerialName("period-start")
            val periodStart: LocalTemplate.NameDescriptionString,
            @SerialName("period-end")
            val periodEnd: LocalTemplate.NameDescriptionString,
            @SerialName("total-usage")
            val totalUsage: LocalTemplate.NameDescriptionString,
            @SerialName("total-amount")
            val totalAmount: LocalTemplate.NameDescriptionString,
            @SerialName("public-usage")
            val publicUsage: LocalTemplate.NameDescriptionString,
            @SerialName("room-a-usage")
            val roomAUsage: LocalTemplate.NameDescriptionString,
            @SerialName("room-b-usage")
            val roomBUsage: LocalTemplate.NameDescriptionString,
            @SerialName("room-c-usage")
            val roomCUsage: LocalTemplate.NameDescriptionString,
        )
    }

    @Serializable
    internal data class CommandWaterBill(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            @SerialName("bill-month")
            val billMonth: LocalTemplate.NameDescriptionString,
            @SerialName("total-usage")
            val totalUsage: LocalTemplate.NameDescriptionString,
            @SerialName("total-amount")
            val totalAmount: LocalTemplate.NameDescriptionString,
        )
    }

    @Serializable
    internal data class CommandRentOverview(
        val name: String,
        val description: String,
        val options: Options
    ) {
        @Serializable
        internal data class Options(
            val month: LocalTemplate.NameDescriptionString,
        )
    }
}