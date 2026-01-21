package tw.xinshou.discord.plugin.economy

import tw.xinshou.discord.plugin.economy.json.DataContainer

/**
 * Class representing a user's economic data.
 *
 * @property id Unique identifier for the user.
 * @property data DataContainer containing the user's balance and total cost.
 */
internal class UserData(
    val id: Long,
    var data: DataContainer = DataContainer(0, 0),
) {
    /**
     * Adds a specified amount of money to the user's balance.
     *
     * @param money Amount to add to the user's balance.
     * @return Updated balance after the addition.
     */
    fun addMoney(money: Int): Int {
        this.data.money += money
        return this.data.money
    }

    /**
     * Removes a specified amount of money from the user's balance and adds it to the cost.
     *
     * @param money Amount to remove from the balance and add to cost.
     * @return Updated balance after the removal.
     */
    fun removeMoneyAddCost(money: Int): Int {
        this.data.money -= money
        this.data.cost += money
        return this.data.money
    }

    /**
     * Sets the user's balance to a specified amount.
     *
     * @param money New balance amount.
     * @return Updated balance.
     */
    fun setMoney(money: Int): Int {
        this.data.money = money
        return this.data.money
    }

    fun addCost(cost: Int): Int {
        this.data.cost += cost
        return this.data.cost
    }

    fun removeCost(cost: Int): Int {
        this.data.cost -= cost
        return this.data.cost
    }

    /**
     * Sets the user's total cost to a specified amount.
     *
     * @param cost New total cost amount.
     * @return Updated total cost.
     */
    fun setCost(cost: Int): Int {
        this.data.cost = cost
        return this.data.cost
    }
}
