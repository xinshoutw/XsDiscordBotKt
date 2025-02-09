package tw.xinshou.plugin.economy

/**
 * Class representing a user's economic data.
 *
 * @property id Unique identifier for the user.
 * @property money Current balance of money the user holds.
 * @property cost Total expenses or costs recorded for the user.
 */
internal class UserData(
    val id: Long,
    var money: Int = 0,
    var cost: Int = 0,
) {
    /**
     * Adds a specified amount of money to the user's balance.
     *
     * @param money Amount to add to the user's balance.
     * @return Updated balance after the addition.
     */
    fun addMoney(money: Int): Int {
        this.money += money
        return this.money
    }

    /**
     * Removes a specified amount of money from the user's balance and adds it to the cost.
     *
     * @param money Amount to remove from the balance and add to cost.
     * @return Updated balance after the removal.
     */
    fun removeMoneyAddCost(money: Int): Int {
        this.money -= money
        this.cost += money
        return this.money
    }

    /**
     * Sets the user's balance to a specified amount.
     *
     * @param money New balance amount.
     * @return Updated balance.
     */
    fun setMoney(money: Int): Int {
        this.money = money
        return this.money
    }

    fun addCost(cost: Int): Int {
        this.cost += cost
        return this.cost
    }

    fun removeCost(cost: Int): Int {
        this.cost -= cost
        return this.cost
    }

    /**
     * Sets the user's total cost to a specified amount.
     *
     * @param money New total cost amount.
     * @return Updated total cost.
     */
    fun setCost(money: Int): Int {
        this.cost = money
        return this.cost
    }
}
