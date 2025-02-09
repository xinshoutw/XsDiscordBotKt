package tw.xinshou.plugin.economy.storage

import com.google.gson.JsonObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.loader.builtin.placeholder.Substitutor
import tw.xinshou.loader.json.JsonObjFileManager
import tw.xinshou.plugin.economy.Economy.Type
import tw.xinshou.plugin.economy.Event.config
import tw.xinshou.plugin.economy.UserData
import kotlin.math.min

/**
 * Manages user data and rankings via a JSON file system.
 */
internal object JsonImpl : IStorage {
    private val userData: MutableMap<Long, UserData> = HashMap()
    private val moneyBoard: MutableList<UserData> = ArrayList()
    private val costBoard: MutableList<UserData> = ArrayList()
    lateinit var json: JsonObjFileManager

    /**
     * Initializes the JSON file by loading existing users or creating new entries.
     */
    override fun init() {
        json.get().keySet().forEach { key ->
            val id = key.toLong()
            val obj = json.computeIfAbsent(key, JsonObject()).asJsonObject
            userData[id] = UserData(id, obj["money"].asInt, obj["cost"].asInt)
        }

        moneyBoard.addAll(userData.values)
        costBoard.addAll(userData.values)
        sortMoneyBoard()
        sortCostBoard()
    }

    /**
     * Queries the economic data of a specific user.
     * Initializes the user data if not present.
     *
     * @param user The user to query.
     * @return UserData for the requested user.
     */
    override fun query(user: User): UserData {
        initUserData(user)
        return userData[user.idLong]!!
    }

    /**
     * Updates the stored data for a specific user.
     *
     * @param user The user data to update.
     */
    override fun update(user: UserData) {
        update(user.id, user.money, user.cost)
    }

    /**
     * Sorts the leaderboard based on money.
     */
    override fun sortMoneyBoard() {
        moneyBoard.sortByDescending { it.money }
    }

    /**
     * Sorts the leaderboard based on cost.
     */
    override fun sortCostBoard() {
        costBoard.sortByDescending { it.cost }
    }

    override fun getEmbedBuilder(
        type: Type,
        embedBuilder: EmbedBuilder,
        descriptionTemplate: String,
        substitutor: Substitutor
    ): EmbedBuilder {
        val board = when (type) {
            Type.Money -> moneyBoard
            Type.Cost -> costBoard
        }

        val count = min(board.size, min(config.boardUserShowLimit, 25))

        return embedBuilder.apply {
            setDescription("")

            for (i in 1..count) {
                val data = board[i - 1]
                appendDescription(
                    substitutor
                        .putAll(
                            "index" to "$i",
                            "name_mention" to "<@${data.id}>",
                            "economy_board" to "${if (type == Type.Money) data.money else data.cost}",
                        ).parse(descriptionTemplate)
                )
            }
        }
    }


    /**
     * Initializes user data if it does not exist in the system.
     *
     * @param user The user for whom data needs to be initialized.
     */
    private fun initUserData(user: User) {
        val id = user.idLong
        if (!userData.containsKey(id)) {
            val data = UserData(id)
            userData[id] = data
            moneyBoard.add(data)
            costBoard.add(data)

            val obj = json.computeIfAbsent(id.toString(), JsonObject()).asJsonObject
            obj.addProperty("money", 0)
            obj.addProperty("cost", 0)
            json.save()
        }
    }

    /**
     * Updates the JSON file with the latest user money and cost data.
     *
     * @param userId The ID of the user to update.
     * @param money The updated amount of money.
     * @param cost The updated amount of cost.
     */
    private fun update(userId: Long, money: Int, cost: Int) {
        val obj = json.getAsJsonObject(userId.toString())
        obj.addProperty("money", money)
        obj.addProperty("cost", cost)
        json.save()
    }
}
