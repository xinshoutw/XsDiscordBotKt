package tw.xinshou.discord.plugin.economy.storage

import com.google.api.services.sheets.v4.model.ValueRange
import tw.xinshou.discord.core.placeholder.Substitutor
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.discord.plugin.api.google.sheet.SheetsService
import tw.xinshou.discord.plugin.api.google.sheet.config.ConfigSerializer
import tw.xinshou.discord.plugin.economy.Economy.Type
import tw.xinshou.discord.plugin.economy.Event.pluginConfig
import tw.xinshou.discord.plugin.economy.Event.pluginDirectory
import tw.xinshou.discord.plugin.economy.UserData
import tw.xinshou.discord.plugin.economy.json.DataContainer
import kotlin.math.min

internal object SheetImpl : IStorage {
    private val spreadsheet = SheetsService(
        ConfigSerializer(pluginConfig.clientId, pluginConfig.clientSecret, pluginConfig.port), pluginDirectory
    ).sheets.spreadsheets().values()

    override fun init() {}

    override fun query(guildId: Long, user: User): UserData {
        val current = query()
        val index = current[0].indexOf(user.idLong)

        return if (index == -1)
            UserData(user.idLong)
        else
            UserData(
                user.idLong,
                DataContainer(
                    money = current[1][index].toString().toInt(),
                    cost = current[2][index].toString().toInt(),
                )
            )
    }

    override fun update(guildId: Long, data: UserData) {
        update(data.id, data.data)
    }

    override fun getEmbedBuilder(
        guildId: Long, type: Type, embedBuilder: EmbedBuilder,
        descriptionTemplate: String, substitutor: Substitutor
    ): EmbedBuilder {
        val board = when (type) {
            Type.Money -> queryAll().sortedByDescending { it.data.money }
            Type.Cost -> queryAll().sortedByDescending { it.data.cost }
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

    override fun sortMoneyBoard() {}
    override fun sortCostBoard() {}

    private fun update(userId: Long, data: DataContainer) {
        val index = indexOfUserId(userId)
        if (index == -1) {
            spreadsheet
                .append(
                    pluginConfig.sheetId, parseRange(pluginConfig.sheetRangeId),
                    ValueRange().setValues(listOf(listOf("$userId", "${data.money}", "${data.cost}")))
                )
                .setValueInputOption("RAW")
                .execute()
        } else {
            spreadsheet
                .update(
                    pluginConfig.sheetId,
                    parseRange(offsetRange(pluginConfig.sheetRangeId, index)),
                    ValueRange().setValues(listOf(listOf("$userId", "${data.money}", "${data.cost}")))
                )
                .setValueInputOption("RAW")
                .execute()
        }
    }

    private fun query(): List<List<Long>> = getBatchRange(
        listOf(
            parseRange(pluginConfig.sheetRangeId),
            parseRange(pluginConfig.sheetRangeMoney),
            parseRange(pluginConfig.sheetRangeCost),
        )
    )

    private fun queryAll(): List<UserData> {
        val data = query()
        return data[0].mapIndexed { index, id ->
            UserData(id, DataContainer(data[1][index].toInt(), data[2][index].toInt()))
        }
    }

    private fun indexOfUserId(userId: Long): Int = getRange(pluginConfig.sheetRangeId).indexOf(userId)

    private fun offsetRange(baseCell: String, offset: Int): String {
        val (column, row) = Regex("([A-Za-z]+)(\\d+)").find(baseCell)!!.destructured
        return "$column${row.toInt() + offset}"
    }

    private fun parseRange(range: String): String = "${pluginConfig.sheetLabel}!${range}"

    private fun getRange(range: String): List<Long> =
        spreadsheet.get(pluginConfig.sheetId, parseRange(range)).execute().values
            .map { it.toString().toLong() }

    private fun getBatchRange(ranges: List<String>): List<List<Long>> {
        val valueRanges = spreadsheet.batchGet(pluginConfig.sheetId).setRanges(ranges).execute().valueRanges
        return valueRanges.map { range ->
            range.values.map { it.toString().toLong() }
        }
    }
}
