package tw.xinshou.plugin.ntustcourse

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.plugin.PluginEvent
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.ntustcourse.command.commandStringSet
import tw.xinshou.plugin.ntustcourse.command.guildCommands

/**
 * Main class for the RentSystem plugin managing configurations, commands, and rental operations.
 */
object Event : PluginEvent(true) {

    override fun load() {
        super.load()
        NtustCourse.start()
        logger.info("NtustCourse loaded.")
    }

    override fun unload() {
        NtustCourse.stop()
        logger.info("NtustCourse unloaded.")
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        NtustCourse.onSlashCommandInteraction(event)
    }
}
