package tw.xinshou.plugin.economy.storage

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.core.builtin.placeholder.Substitutor
import tw.xinshou.core.json.JsonFileManager
import tw.xinshou.plugin.economy.Economy.Type
import tw.xinshou.plugin.economy.Event.config
import tw.xinshou.plugin.economy.UserData
import tw.xinshou.plugin.economy.json.DataContainer
import tw.xinshou.plugin.economy.json.JsonDataClass
import kotlin.math.min

/**
 * Manages user data and rankings via a JSON file system.
 */
internal object JsonImpl : IStorage {
    private val userData: MutableMap<Long, UserData> = HashMap()
    private val moneyBoard: MutableList<UserData> = ArrayList()
    private val costBoard: MutableList<UserData> = ArrayList()
    lateinit var jsonFileManager: JsonFileManager<JsonDataClass>

    /**
     * Initializes the JSON file by loading existing users or creating new entries.
     */
    override fun init() {
        jsonFileManager.data.forEach { key, data ->
            val id = key.toLong()
            userData[id] = UserData(id, data)
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
        update(user.id, user.data)
    }

    /**
     * Sorts the leaderboard based on money.
     */
    override fun sortMoneyBoard() {
        moneyBoard.sortByDescending { it.data.money }
    }

    /**
     * Sorts the leaderboard based on cost.
     */
    override fun sortCostBoard() {
        costBoard.sortByDescending { it.data.cost }
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

        val count = min(board.size, config.boardUserShowLimit)

        return embedBuilder.apply {
            setDescription("")

            for (i in 1..count) {
                val userData = board[i - 1]
                appendDescription(
                    substitutor
                        .putAll(
                            "index" to "$i",
                            "name_mention" to "<@${userData.id}>",
                            "economy_board" to "${if (type == Type.Money) userData.data.money else userData.data.cost}",
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

            jsonFileManager.data.put(id.toString(), DataContainer(0, 0))
            jsonFileManager.save()
        }
    }

    /**
     * Updates the JSON file with the latest user money and cost data.
     *
     * @param userId The ID of the user to update.
     * @param data The updated DataContainer.
     */
    private fun update(userId: Long, data: DataContainer) {
        jsonFileManager.data.put(userId.toString(), data)
        jsonFileManager.save()
    }
}
