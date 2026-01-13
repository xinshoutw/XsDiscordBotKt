package tw.xinshou.plugin.economy

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.core.builtin.messagecreator.MessageCreator
import tw.xinshou.core.builtin.placeholder.Placeholder
import tw.xinshou.core.builtin.placeholder.Substitutor
import tw.xinshou.plugin.economy.Event.pluginDirectory
import tw.xinshou.plugin.economy.Event.storageManager

internal object MessageReplier {
    private var creator = MessageCreator(
        pluginDirectory,
        DiscordLocale.CHINESE_TAIWAN
    )

    internal fun reload() {
        creator = MessageCreator(
            pluginDirectory,
            DiscordLocale.CHINESE_TAIWAN
        )
    }

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
        val substitutor = user.let { Placeholder.get(it) }
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