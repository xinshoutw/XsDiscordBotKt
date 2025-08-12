package tw.xinshou.plugin.feedbacker

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.loader.base.BotLoader.jdaBot
import tw.xinshou.loader.localizations.StringLocalizer
import tw.xinshou.loader.plugin.PluginEventConfigure
import tw.xinshou.loader.util.GlobalUtil
import tw.xinshou.plugin.feedbacker.command.CmdFileSerializer
import tw.xinshou.plugin.feedbacker.command.guildCommands
import tw.xinshou.plugin.feedbacker.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>
    internal lateinit var globalLocale: DiscordLocale

    override fun load() {
        super.load()

        globalLocale = DiscordLocale.from(config.language)

        jdaBot.getGuildById(config.guildId)?.let { guild ->
            Feedbacker.guild = guild
            Feedbacker.submitChannel = guild.getTextChannelById(config.submitChannelId)!!
        }

        localizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun reload() {
        super.reload()

        globalLocale = DiscordLocale.from(config.language)

        jdaBot.getGuildById(config.guildId)?.let { guild ->
            Feedbacker.guild = guild
            Feedbacker.submitChannel = guild.getTextChannelById(config.submitChannelId)!!
        }

        localizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )
    }

    override fun guildCommands(): Array<CommandData> = guildCommands(localizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "feedbacker")) return
        Feedbacker.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Feedbacker.onButtonInteraction(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (GlobalUtil.checkModalIdPrefix(event, componentPrefix)) return
        Feedbacker.onModalInteraction(event)
    }
}
