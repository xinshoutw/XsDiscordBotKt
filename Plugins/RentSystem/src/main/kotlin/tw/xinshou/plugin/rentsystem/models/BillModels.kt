package tw.xinshou.plugin.rentsystem.models

import kotlinx.serialization.Serializable

/**
 * 代表單一帳單項目，例如 "A房電費"
 * @param name 項目名稱
 * @param cost 費用金額
 */
@Serializable
data class BillItem(val name: String, val cost: Double)

/**
 * 彙總最終的帳單數據，用於產生圖表
 * @param powerItems 所有電費相關的項目列表
 * @param waterItems 所有水費相關的項目列表
 */
@Serializable
data class FinalBill(val powerItems: List<BillItem>, val waterItems: List<BillItem>)

/**
 * 每日電表讀數記錄
 * @param meterId 電表ID
 * @param meterName 電表名稱
 * @param reading 電表讀數 (kWh)
 * @param recordDate 記錄日期
 * @param recordedBy 記錄者Discord ID
 * @param recordedAt 記錄時間
 */
@Serializable
data class DailyElectricityReading(
    val meterId: String,
    val meterName: String,
    val reading: Double,
    val recordDate: String, // LocalDate serialized as string
    val recordedBy: Long,
    val recordedAt: String // LocalDateTime serialized as string
)

/**
 * 兩個月一次的電費帳單記錄
 * @param billPeriodStart 帳單期間開始日期
 * @param billPeriodEnd 帳單期間結束日期
 * @param totalUsage 總用電量 (kWh)
 * @param totalAmount 總金額 (包含雜費與減免後的結果)
 * @param meterReadings 各電表的用電量記錄
 * @param recordedBy 記錄者Discord ID
 * @param recordedAt 記錄時間
 */
@Serializable
data class BiMonthlyElectricityBill(
    val billPeriodStart: String, // LocalDate serialized as string
    val billPeriodEnd: String, // LocalDate serialized as string
    val totalUsage: Double,
    val totalAmount: Double,
    val meterReadings: Map<String, Double>, // meterId -> usage
    val recordedBy: Long,
    val recordedAt: String // LocalDateTime serialized as string
)

/**
 * 每月水費帳單記錄
 * @param billMonth 帳單月份 (yyyy-MM格式)
 * @param totalUsage 總用水量 (立方米)
 * @param totalAmount 總金額 (包含雜費與減免後的結果)
 * @param recordedBy 記錄者Discord ID
 * @param recordedAt 記錄時間
 */
@Serializable
data class MonthlyWaterBill(
    val billMonth: String, // yyyy-MM format
    val totalUsage: Double,
    val totalAmount: Double,
    val recordedBy: Long,
    val recordedAt: String // LocalDateTime serialized as string
)

/**
 * 成員完整資料結構
 * @param roomId 房間ID
 * @param roomOwnerName 房間擁有者暱稱
 * @param roomOwnerDiscordId 房間擁有者Discord ID
 * @param monthlyRent 每月租金
 * @param electricityMeterIds 關聯的電表ID列表
 * @param monthlyData 每月的費用分攤資料
 */
@Serializable
data class MemberData(
    val roomId: String,
    val roomOwnerName: String,
    val roomOwnerDiscordId: Long,
    val monthlyRent: Long,
    val electricityMeterIds: List<String>,
    val monthlyData: Map<String, MonthlyMemberData> // month (yyyy-MM) -> data
)

/**
 * 成員每月資料
 * @param month 月份 (yyyy-MM格式)
 * @param rentPaid 租金是否已繳納
 * @param electricityCost 電費分攤金額
 * @param waterCost 水費分攤金額
 * @param miscCost 雜費分攤金額
 * @param internetCost 網路費分攤金額
 * @param totalCost 總費用
 * @param electricityUsage 電力使用量 (kWh)
 * @param waterUsage 水使用量分攤 (立方米)
 */
@Serializable
data class MonthlyMemberData(
    val month: String,
    val rentPaid: Boolean = false,
    val electricityCost: Double = 0.0,
    val waterCost: Double = 0.0,
    val miscCost: Double = 0.0,
    val internetCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val electricityUsage: Double = 0.0,
    val waterUsage: Double = 0.0
)

/**
 * 月份統計資料
 * @param month 月份 (yyyy-MM格式)
 * @param totalElectricityCost 總電費
 * @param totalWaterCost 總水費
 * @param totalMiscCost 總雜費
 * @param totalInternetCost 總網路費
 * @param totalElectricityUsage 總用電量
 * @param totalWaterUsage 總用水量
 * @param memberData 各成員資料
 * @param lastUpdated 最後更新時間
 */
@Serializable
data class MonthlyStatistics(
    val month: String,
    val totalElectricityCost: Double = 0.0,
    val totalWaterCost: Double = 0.0,
    val totalMiscCost: Double = 0.0,
    val totalInternetCost: Double = 0.0,
    val totalElectricityUsage: Double = 0.0,
    val totalWaterUsage: Double = 0.0,
    val memberData: Map<String, MonthlyMemberData>, // roomId -> data
    val lastUpdated: String // LocalDateTime serialized as string
)
