package tw.xinshou.plugin.economy.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigSerializer(
    val enabled: Boolean = false,
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
) {
    init {
        require(port in 1..65535) { "port must be between 1 and 65535" }
        require(adminId.isNotEmpty()) { "adminId must not be empty" }
        require(boardUserShowLimit in 1..25) { "boardUserShowLimit must be between 1 and 25" }
    }
}
