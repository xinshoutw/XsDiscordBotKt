package tw.xserver.plugin.economy.storage

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.plugin.economy.Economy.Type
import tw.xserver.plugin.economy.UserData

internal interface IStorage {
    fun init()
    fun query(user: User): UserData
    fun update(data: UserData)
    fun getEmbedBuilder(
        type: Type,
        embedBuilder: EmbedBuilder,
        descriptionTemplate: String,
        substitutor: Substitutor
    ): EmbedBuilder

    fun sortMoneyBoard()
    fun sortCostBoard()
}