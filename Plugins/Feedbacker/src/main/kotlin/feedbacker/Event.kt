package tw.xinshou.discord.plugin.feedbacker

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.feedbacker.command.CmdFileSerializer
import tw.xinshou.discord.plugin.feedbacker.command.guildCommands
import tw.xinshou.discord.plugin.feedbacker.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>
    internal lateinit var globalLocale: DiscordLocale

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("Feedbacker is disabled.")
            return
        }

        globalLocale = DiscordLocale.from(config.language)

        localizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            logger.warn("Feedbacker is disabled.")
            return
        }

        globalLocale = DiscordLocale.from(config.language)

        localizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        Feedbacker.reload()
    }

    override fun guildCommands(): Array<CommandData> {
        return if (!config.enabled) {
            emptyArray()
        } else {
            guildCommands(localizer)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkCommandString(event, "feedbacker")) return
        Feedbacker.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Feedbacker.onButtonInteraction(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkModalIdPrefix(event, componentPrefix)) return
        Feedbacker.onModalInteraction(event)
    }
}
