package tw.xinshou.discord.plugin.botinfo

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.discord.core.builtin.messagecreator.MessageCreator
import tw.xinshou.discord.core.builtin.placeholder.Placeholder
import tw.xinshou.discord.plugin.botinfo.Event.pluginDirectory

internal object BotInfo {
    private var messageCreator = MessageCreator(
        pluginDirectory,
        DiscordLocale.CHINESE_TAIWAN
    )

    internal fun reload() {
        messageCreator = MessageCreator(
            pluginDirectory,
            DiscordLocale.CHINESE_TAIWAN
        )
    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val memberCounts = event.jda.guilds.sumOf { it.memberCount }

        Placeholder.globalSubstitutor.put("member_counts", "$memberCounts")
        Placeholder.globalSubstitutor.put("guild_counts", "${event.jda.guilds.size}")

        event.hook.editOriginal(
            MessageEditData.fromCreateData(
                messageCreator.getCreateBuilder(
                    "bot-info",
                    event.userLocale
                ).build()
            )
        ).queue()
    }
}