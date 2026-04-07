package tw.xinshou.discord.plugin.economy

import core.i18n.MessageTemplate
import core.placeholder.Substitutor
import core.placeholder.withUser
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.discord.plugin.economy.Event.pluginDirectory
import tw.xinshou.discord.plugin.economy.Event.storageManager
import java.io.File

internal object MessageReplier {
    private var template = MessageTemplate(
        langDir = File(pluginDirectory, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN,
    )

    internal fun reload() {
        template = MessageTemplate(
            langDir = File(pluginDirectory, "lang"),
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
        )
    }

    fun getNoPermissionMessageData(locale: DiscordLocale): MessageEditData =
        MessageEditData.fromCreateData(
            template.buildCreate("no-permission", locale).build()
        )

    fun getMessageEditData(key: String, locale: DiscordLocale, substitutor: Substitutor): MessageEditData =
        MessageEditData.fromCreateData(
            template.buildCreate(key, locale, substitutor).build()
        )

    fun replyBoard(key: String, guildId: Long, user: User, userLocale: DiscordLocale): MessageEditData {
        val substitutor = Substitutor().withUser(user)
        val builder = template.buildCreate(key, userLocale, substitutor)
        val type: Economy.Type = when (key) {
            "top-money" -> Economy.Type.Money
            else -> Economy.Type.Cost
        }

        // Note: In v4, MessageTemplate doesn't expose getMessageData directly.
        // The embed description template needs to be handled differently.
        // For now, we build the embed from the storage manager directly.
        val descriptionTemplate = "%index%. %name_mention% - %economy_board%"

        builder.setEmbeds(
            storageManager.getEmbedBuilder(
                guildId,
                type,
                EmbedBuilder(builder.embeds[0]),
                descriptionTemplate,
                substitutor
            ).build()
        )

        return MessageEditData.fromCreateData(builder.build())
    }
}
