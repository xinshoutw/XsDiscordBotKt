package tw.xinshou.discord.plugin.ntustcourse

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.ntustcourse.command.commandStringSet
import tw.xinshou.discord.plugin.ntustcourse.command.guildCommands
import tw.xinshou.discord.plugin.ntustcourse.config.ConfigSerializer

/**
 * Main class for the RentSystem plugin managing configurations, commands, and rental operations.
 */
object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("NtustCourse is disabled.")
            return
        }

        NtustCourse.start()
        logger.info("NtustCourse loaded.")
    }

    override fun unload() {
        if (config.enabled) {
            NtustCourse.stop()
        }
        super.unload()
        logger.info("NtustCourse unloaded.")
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            NtustCourse.stop()
            logger.warn("NtustCourse is disabled.")
            return
        }

        NtustCourse.stop()
        NtustCourse.start()
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        NtustCourse.onSlashCommandInteraction(event)
    }
}
