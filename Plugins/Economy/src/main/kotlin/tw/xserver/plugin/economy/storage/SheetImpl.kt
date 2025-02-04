package tw.xserver.plugin.economy.storage

import com.google.api.services.sheets.v4.model.ValueRange
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.plugin.api.google.sheet.SheetsService
import tw.xserver.plugin.api.google.sheet.serializer.AuthConfigSerializer
import tw.xserver.plugin.economy.Economy.Type
import tw.xserver.plugin.economy.Event.PLUGIN_DIR_FILE
import tw.xserver.plugin.economy.Event.config
import tw.xserver.plugin.economy.UserData
import kotlin.math.min

/**
 * Manages interactions with a Google Sheets spreadsheet for economy-related data operations.
 */
internal object SheetImpl : IStorage {
    private val spreadsheet = SheetsService(
        AuthConfigSerializer(config.clientId, config.clientSecret, config.port), PLUGIN_DIR_FILE
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
                money = current[1][index].toString().toInt(),
                cost = current[2][index].toString().toInt(),
            )
    }

    /**
     * Updates the spreadsheet with the latest user data.
     *
     * @param data The user data to be updated.
     */
    override fun update(data: UserData) {
        update(data.id, data.money, data.cost)
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
            Type.Money -> queryAll().sortedByDescending { it.money }
            Type.Cost -> queryAll().sortedByDescending { it.cost }
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

    override fun sortMoneyBoard() {}

    override fun sortCostBoard() {}

    private fun update(userId: Long, money: Int, cost: Int) {
        val index = indexOfUserId(userId)
        if (index == -1) {
            // User not in the sheet, append new entry
            spreadsheet
                .append(
                    config.sheetId, parseRange(config.sheetRangeId),
                    ValueRange().setValues(listOf(listOf("$userId", "$money", "$cost")))
                )
                .setValueInputOption("RAW")
                .execute()
        } else {
            // Update existing entry
            spreadsheet
                .update(
                    config.sheetId,
                    parseRange(offsetRange(config.sheetRangeId, index)),
                    ValueRange().setValues(listOf(listOf("$userId", "$money", "$cost")))
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
            UserData(id, data[1][index].toInt(), data[2][index].toInt())
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
