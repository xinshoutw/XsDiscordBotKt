package tw.xinshou.discord.plugin.economy.storage

import com.google.api.services.sheets.v4.model.ValueRange
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.plugin.api.google.sheet.SheetsService
import tw.xinshou.discord.plugin.api.google.sheet.config.ConfigSerializer
import tw.xinshou.discord.plugin.economy.Economy.Type
import tw.xinshou.discord.plugin.economy.Event.config
import tw.xinshou.discord.plugin.economy.Event.pluginDirectory
import tw.xinshou.discord.plugin.economy.UserData
import tw.xinshou.discord.plugin.economy.json.DataContainer
import kotlin.math.min

/**
 * Manages interactions with a Google Sheets spreadsheet for economy-related data operations.
 */
internal object SheetImpl : IStorage {
    private val spreadsheet = SheetsService(
        ConfigSerializer(config.clientId, config.clientSecret, config.port), pluginDirectory
    ).sheets.spreadsheets().values()

    override fun init() {}

    /**
     * Queries the Google Sheet to retrieve or initialize economy data for a specific user.
     *
     * @param user The Discord user to query.
     * @return UserData containing the user's economy data.
     */
    override fun query(user: User): UserData {
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

    /**
     * Updates the spreadsheet with the latest user data.
     *
     * @param user The user data to be updated.
     */
    override fun update(user: UserData) {
        update(user.id, user.data)
    }

    /**
     * Constructs an embed builder to display a leaderboard based on user data type (Money or Cost).
     *
     * @param type The type of economic data to display.
     * @return EmbedBuilder configured to display the leaderboard.
     */
    override fun getEmbedBuilder(
        type: Type,
        embedBuilder: EmbedBuilder,
        descriptionTemplate: String,
        substitutor: Substitutor
    ): EmbedBuilder {
        val board = when (type) {
            Type.Money -> queryAll().sortedByDescending { it.data.money }
            Type.Cost -> queryAll().sortedByDescending { it.data.cost }
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

    override fun sortMoneyBoard() {}

    override fun sortCostBoard() {}

    private fun update(userId: Long, data: DataContainer) {
        val index = indexOfUserId(userId)
        if (index == -1) {
            // User not in the sheet, append new entry
            spreadsheet
                .append(
                    config.sheetId, parseRange(config.sheetRangeId),
                    ValueRange().setValues(listOf(listOf("$userId", "${data.money}", "${data.cost}")))
                )
                .setValueInputOption("RAW")
                .execute()
        } else {
            // Update existing entry
            spreadsheet
                .update(
                    config.sheetId,
                    parseRange(offsetRange(config.sheetRangeId, index)),
                    ValueRange().setValues(listOf(listOf("$userId", "${data.money}", "${data.cost}")))
                )
                .setValueInputOption("RAW")
                .execute()
        }
    }

    private fun query(): List<List<Long>> = getBatchRange(
        listOf(
            parseRange(config.sheetRangeId),
            parseRange(config.sheetRangeMoney),
            parseRange(config.sheetRangeCost),
        )
    )

    private fun queryAll(): List<UserData> {
        val data = query()
        return data[0].mapIndexed { index, id ->
            UserData(id, DataContainer(data[1][index].toInt(), data[2][index].toInt()))
        }
    }

    private fun indexOfUserId(userId: Long): Int = getRange(config.sheetRangeId).indexOf(userId)

    private fun offsetRange(baseCell: String, offset: Int): String {
        val (column, row) = Regex("([A-Za-z]+)(\\d+)").find(baseCell)!!.destructured
        return "$column${row.toInt() + offset}"
    }

    private fun parseRange(range: String): String = "${config.sheetLabel}!${range}"

    private fun getRange(range: String): List<Long> =
        spreadsheet.get(config.sheetId, parseRange(range)).execute().values
            .map { it.toString().toLong() }

    private fun getBatchRange(ranges: List<String>): List<List<Long>> {
        val valueRanges = spreadsheet.batchGet(config.sheetId).setRanges(ranges).execute().valueRanges
        return valueRanges.map { range ->
            range.values.map { it.toString().toLong() }
        }
    }
}
