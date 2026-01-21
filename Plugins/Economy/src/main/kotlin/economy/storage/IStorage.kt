package tw.xinshou.discord.plugin.economy.storage

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.discord.core.builtin.placeholder.Substitutor
import tw.xinshou.discord.plugin.economy.Economy.Type
import tw.xinshou.discord.plugin.economy.UserData

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