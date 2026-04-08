package tw.xinshou.discord.plugin.economy.storage

import tw.xinshou.discord.core.placeholder.Substitutor
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import tw.xinshou.discord.plugin.economy.Economy.Type
import tw.xinshou.discord.plugin.economy.UserData

internal interface IStorage {
    fun init()
    fun query(guildId: Long, user: User): UserData
    fun update(guildId: Long, data: UserData)
    fun getEmbedBuilder(
        guildId: Long,
        type: Type,
        embedBuilder: EmbedBuilder,
        descriptionTemplate: String,
        substitutor: Substitutor
    ): EmbedBuilder

    fun sortMoneyBoard()
    fun sortCostBoard()
}
