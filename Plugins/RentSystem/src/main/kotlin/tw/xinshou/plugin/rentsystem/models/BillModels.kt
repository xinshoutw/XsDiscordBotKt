package tw.xinshou.plugin.rentsystem.models

/**
 * 代表單一帳單項目，例如 "A房電費"
 * @param name 項目名稱
 * @param cost 費用金額
 */
data class BillItem(val name: String, val cost: Double)

/**
 * 彙總最終的帳單數據，用於產生圖表
 * @param powerItems 所有電費相關的項目列表
 * @param waterItems 所有水費相關的項目列表
 */
data class FinalBill(val powerItems: List<BillItem>, val waterItems: List<BillItem>)
