package tw.xinshou.plugin.rentsystem

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import tw.xinshou.core.plugin.PluginEventConfigure
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.rentsystem.command.commandStringSet
import tw.xinshou.plugin.rentsystem.config.ConfigSerializer

/**
 * Main class for the RentSystem plugin managing configurations, commands, and rental operations.
 */
object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
//    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

//        localizer = StringLocalizer(
//            pluginDirFile = pluginDirectory,
//            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
//            clazzSerializer = CmdFileSerializer::class,
//        )

        logger.info("RentSystem loaded.")
    }

    //    override fun guildCommands(): Array<CommandData> = guildCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkSlashCommand(event, commandStringSet)) return
        RentSystem.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        RentSystem.onButtonInteraction(event)
    }
}
