package tw.xinshou.plugin.botinfo

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tw.xinshou.loader.builtin.messagecreator.MessageCreator
import tw.xinshou.loader.builtin.placeholder.Placeholder
import tw.xinshou.plugin.botinfo.Event.PLUGIN_DIR_FILE
import java.io.File

internal object BotInfo {
    private val creator = MessageCreator(
        langDirFile = File(PLUGIN_DIR_FILE, "lang"),
        defaultLocale = DiscordLocale.CHINESE_TAIWAN
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