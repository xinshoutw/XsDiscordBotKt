package tw.xinshou.discord.plugin.economy.storage

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.core.json.JsonGuildFileManager
import tw.xinshou.discord.plugin.economy.Economy.Type
import tw.xinshou.discord.plugin.economy.Event.config
import tw.xinshou.discord.plugin.economy.UserData
import tw.xinshou.discord.plugin.economy.json.DataContainer
import tw.xinshou.discord.plugin.economy.json.JsonDataClass
import kotlin.math.min

/**
 * Manages user data and rankings via a JSON file system.
 */
internal object JsonImpl : IStorage {
    private data class GuildData(
        val userData: MutableMap<Long, UserData> = HashMap(),
        val moneyBoard: MutableList<UserData> = ArrayList(),
        val costBoard: MutableList<UserData> = ArrayList()
    )

    private val guildDataMap: MutableMap<Long, GuildData> = HashMap()
    lateinit var jsonGuildFileManager: JsonGuildFileManager<JsonDataClass>

    /**
     * Initializes the JSON file by loading existing users or creating new entries.
     */

    override fun init() {
        guildDataMap.clear()
        jsonGuildFileManager.mapper.forEach { (guildId, manager) ->
            val guildData = GuildData()

            manager.data.forEach { (key, data) ->
                val userId = key.toLongOrNull() ?: return@forEach
                val userData = UserData(userId, data)
                guildData.userData[userId] = userData
            }

            guildData.moneyBoard.addAll(guildData.userData.values)
            guildData.costBoard.addAll(guildData.userData.values)
            guildDataMap[guildId] = guildData
        }

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
    override fun query(guildId: Long, user: User): UserData {
        initUserData(guildId, user)
        return getGuildData(guildId).userData[user.idLong]!!
    }

    /**
     * Updates the stored data for a specific user.
     *
     * @param data The user data to update.
     */
    override fun update(guildId: Long, data: UserData) {
        update(guildId, data.id, data.data)
    }

    /**
     * Sorts the leaderboard based on money.
     */
    override fun sortMoneyBoard() {
        guildDataMap.values.forEach { guildData ->
            guildData.moneyBoard.sortByDescending { it.data.money }
        }
    }

    /**
     * Sorts the leaderboard based on cost.
     */
    override fun sortCostBoard() {
        guildDataMap.values.forEach { guildData ->
            guildData.costBoard.sortByDescending { it.data.cost }
        }
    }

    override fun getEmbedBuilder(
        guildId: Long,
        type: Type,
        embedBuilder: EmbedBuilder,
        descriptionTemplate: String,
        substitutor: Substitutor
    ): EmbedBuilder {
        val guildData = getGuildData(guildId)
        val board = when (type) {
            Type.Money -> guildData.moneyBoard
            Type.Cost -> guildData.costBoard
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
    private fun initUserData(guildId: Long, user: User) {
        val guildData = getGuildData(guildId)
        val id = user.idLong
        if (!guildData.userData.containsKey(id)) {
            val data = UserData(id)
            guildData.userData[id] = data
            guildData.moneyBoard.add(data)
            guildData.costBoard.add(data)

            val manager = jsonGuildFileManager[guildId]
            manager.data[id.toString()] = DataContainer(0, 0)
            manager.save()
        }
    }

    /**
     * Updates the JSON file with the latest user money and cost data.
     *
     * @param userId The ID of the user to update.
     * @param data The updated DataContainer.
     */
    private fun update(guildId: Long, userId: Long, data: DataContainer) {
        val manager = jsonGuildFileManager[guildId]
        manager.data[userId.toString()] = data
        manager.save()
    }

    private fun getGuildData(guildId: Long): GuildData {
        return guildDataMap.getOrPut(guildId) {
            val guildData = GuildData()
            val manager = jsonGuildFileManager[guildId]

            manager.data.forEach { (key, data) ->
                val userId = key.toLongOrNull() ?: return@forEach
                val userData = UserData(userId, data)
                guildData.userData[userId] = userData
            }

            guildData.moneyBoard.addAll(guildData.userData.values)
            guildData.costBoard.addAll(guildData.userData.values)
            guildData.moneyBoard.sortByDescending { it.data.money }
            guildData.costBoard.sortByDescending { it.data.cost }
            guildData
        }
    }
}
