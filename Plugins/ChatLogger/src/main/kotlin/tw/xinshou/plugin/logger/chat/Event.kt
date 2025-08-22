package tw.xinshou.plugin.logger.chat

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.base.BotLoader.jdaBot
import tw.xinshou.core.localizations.StringLocalizer
import tw.xinshou.core.plugin.PluginEventConfigure
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.logger.chat.JsonManager.dataMap
import tw.xinshou.plugin.logger.chat.command.CmdFileSerializer
import tw.xinshou.plugin.logger.chat.command.PlaceholderSerializer
import tw.xinshou.plugin.logger.chat.command.guildCommands
import tw.xinshou.plugin.logger.chat.config.ConfigSerializer


object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var registerLocalizer: StringLocalizer<CmdFileSerializer>
    internal lateinit var placeholderLocalizer: StringLocalizer<PlaceholderSerializer>

    override fun load() {
        super.load()

        registerLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            fileName = "register.yaml"
        )

        placeholderLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = PlaceholderSerializer::class,
            fileName = "placeholder.yaml"
        )
    }

    override fun unload() {
        DbManager.disconnect()
        dataMap.clear()

        logger.info("ChatLogger unloaded.")
    }

    override fun reload() {
        super.reload()

        registerLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
            fileName = "register.yaml"
        )

        placeholderLocalizer = StringLocalizer(
            pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = PlaceholderSerializer::class,
            fileName = "placeholder.yaml"
        )
    }


    override fun guildCommands(): Array<CommandData> = guildCommands(registerLocalizer)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "chat-logger setting")) return
        ChatLogger.onSlashCommandInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        ChatLogger.onEntitySelectInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        ChatLogger.onButtonInteraction(event)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild || event.author == jdaBot.selfUser) return
        ChatLogger.onMessageReceived(event)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (!event.isFromGuild || event.author == jdaBot.selfUser) return
        ChatLogger.onMessageUpdate(event)
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (!event.isFromGuild) return
        ChatLogger.onMessageDelete(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        ChatLogger.onGuildLeave(event)
    }
}
