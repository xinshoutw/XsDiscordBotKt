package tw.xinshou.discord.plugin.economy.storage

import tw.xinshou.discord.core.placeholder.Substitutor
import tw.xinshou.discord.core.util.GuildJsonFile
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.discord.plugin.economy.Economy.Type
import tw.xinshou.discord.plugin.economy.Event.pluginConfig
import tw.xinshou.discord.plugin.economy.Event.pluginDirectory
import tw.xinshou.discord.plugin.economy.UserData
import tw.xinshou.discord.plugin.economy.json.DataContainer
import tw.xinshou.discord.plugin.economy.json.JsonDataClass
import java.io.File
import kotlin.math.min

internal object JsonImpl : IStorage {
    private data class GuildData(
        val userData: MutableMap<Long, UserData> = HashMap(),
        val moneyBoard: MutableList<UserData> = ArrayList(),
        val costBoard: MutableList<UserData> = ArrayList()
    )

    private val guildDataMap: MutableMap<Long, GuildData> = HashMap()
    lateinit var jsonGuildFileManager: GuildJsonFile<JsonDataClass>

    override fun init() {
        guildDataMap.clear()

        val dataFolder = File(pluginDirectory, "data")
        dataFolder.listFiles()?.filter { it.isFile && it.extension == "json" }?.forEach { file ->
            val guildId = file.nameWithoutExtension.toLongOrNull() ?: return@forEach
            val guildData = GuildData()

            val jsonFile = jsonGuildFileManager[guildId]
            jsonFile.data.forEach { (key, data) ->
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

    override fun query(guildId: Long, user: User): UserData {
        initUserData(guildId, user)
        return getGuildData(guildId).userData[user.idLong]!!
    }

    override fun update(guildId: Long, data: UserData) {
        update(guildId, data.id, data.data)
    }

    override fun sortMoneyBoard() {
        guildDataMap.values.forEach { guildData ->
            guildData.moneyBoard.sortByDescending { it.data.money }
        }
    }

    override fun sortCostBoard() {
        guildDataMap.values.forEach { guildData ->
            guildData.costBoard.sortByDescending { it.data.cost }
        }
    }

    override fun getEmbedBuilder(
        guildId: Long, type: Type, embedBuilder: EmbedBuilder,
        descriptionTemplate: String, substitutor: Substitutor
    ): EmbedBuilder {
        val guildData = getGuildData(guildId)
        val board = when (type) {
            Type.Money -> guildData.moneyBoard
            Type.Cost -> guildData.costBoard
        }

        val count = min(board.size, pluginConfig.boardUserShowLimit)

        return embedBuilder.apply {
            setDescription("")
            for (i in 1..count) {
                val userData = board[i - 1]
                appendDescription(
                    substitutor.putAll(
                        "index" to "$i",
                        "name_mention" to "<@${userData.id}>",
                        "economy_board" to "${if (type == Type.Money) userData.data.money else userData.data.cost}",
                    ).parse(descriptionTemplate)
                )
            }
        }
    }

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
