package tw.xserver.plugin.economy

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xserver.loader.builtin.messagecreator.MessageCreator
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.plugin.economy.Event.PLUGIN_DIR_FILE
import tw.xserver.plugin.economy.Event.storageManager
import java.io.File

internal object MessageReplier {
    private val creator = MessageCreator(
        File(PLUGIN_DIR_FILE, "lang"),
        DiscordLocale.CHINESE_TAIWAN,
        listOf(
            "no-permission",
            "balance",

            "add-money",
            "remove-money",
            "set-money",
            "top-money",

            "add-cost",
            "remove-cost",
            "set-cost",
            "top-cost",
        )
    )

    fun getNoPermissionMessageData(
        locale: DiscordLocale,
    ): MessageEditData =
        MessageEditData.fromCreateData(
            creator.getCreateBuilder("no-permission", locale).build()
        )

    fun getMessageEditData(
        key: String,
        locale: DiscordLocale,
        substitutor: Substitutor
    ): MessageEditData =
        MessageEditData.fromCreateData(
            creator.getCreateBuilder(
                key, locale, substitutor
            ).build()
        )

    fun replyBoard(
        key: String,
        user: User,
        userLocale: DiscordLocale,
    ): MessageEditData {
        val substitutor = user.let { Placeholder.getSubstitutor(it) }
        val builder = creator.getCreateBuilder(key, userLocale, substitutor)
        val type: Economy.Type = when (key) {
            "top-money" -> Economy.Type.Money
            else -> Economy.Type.Cost
        }

        builder.setEmbeds(
            storageManager.getEmbedBuilder(
                type,
                EmbedBuilder(builder.embeds[0]),
                creator.getMessageData(key, userLocale).embeds[0].description!!,
                substitutor
            ).build()
        )

        return MessageEditData.fromCreateData(builder.build())
    }
}