package tw.xinshou.plugin.botinfo

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.core.builtin.messagecreator.MessageCreator
import tw.xinshou.core.builtin.placeholder.Placeholder
import tw.xinshou.plugin.botinfo.Event.pluginDirectory

internal object BotInfo {
    private val creator = MessageCreator(
        pluginDirectory,
        DiscordLocale.CHINESE_TAIWAN
    )

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val memberCounts = event.jda.guilds.sumOf { it.memberCount }

        Placeholder.globalSubstitutor.put("member_counts", "$memberCounts")
        Placeholder.globalSubstitutor.put("guild_counts", "${event.jda.guilds.size}")

        event.hook.editOriginal(
            MessageEditData.fromCreateData(
                creator.getCreateBuilder(
                    "bot-info",
                    event.userLocale
                ).build()
            )
        ).queue()
    }
}