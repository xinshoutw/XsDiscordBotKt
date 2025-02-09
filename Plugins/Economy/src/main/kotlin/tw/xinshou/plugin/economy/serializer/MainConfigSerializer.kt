package tw.xinshou.plugin.economy.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MainConfigSerializer(
    @SerialName("client_id")
    val clientId: String = "",

    @SerialName("client_secret")
    val clientSecret: String = "",
    val port: Int = 8888,

    @SerialName("sheet_id")
    val sheetId: String = "",

    @SerialName("sheet_label")
    val sheetLabel: String = "",

    @SerialName("sheet_range_id")
    val sheetRangeId: String = "",

    @SerialName("sheet_range_money")
    val sheetRangeMoney: String = "",

    @SerialName("sheet_range_cost")
    val sheetRangeCost: String = "",

    @SerialName("admin_id")
    val adminId: List<Long> = emptyList(),

    @SerialName("board_user_show_limit")
    val boardUserShowLimit: Int = 10
)
