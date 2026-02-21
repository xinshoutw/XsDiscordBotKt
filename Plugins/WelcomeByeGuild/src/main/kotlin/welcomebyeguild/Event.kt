package tw.xinshou.discord.plugin.welcomebyeguild

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.welcomebyeguild.command.CmdFileSerializer
import tw.xinshou.discord.plugin.welcomebyeguild.command.commandNameSet
import tw.xinshou.discord.plugin.welcomebyeguild.command.guildCommands
import tw.xinshou.discord.plugin.welcomebyeguild.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("WelcomeByeGuild is disabled.")
            return
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        WelcomeByeGuild.load()
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            logger.warn("WelcomeByeGuild is disabled.")
            return
        }

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = DiscordLocale.CHINESE_TAIWAN,
            clazzSerializer = CmdFileSerializer::class,
        )

        WelcomeByeGuild.reload()
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
        if (GlobalUtil.checkSlashCommand(event, commandNameSet)) return
        WelcomeByeGuild.onSlashCommandInteraction(event)
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (!config.enabled) return
        WelcomeByeGuild.onGuildMemberJoin(event)
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        if (!config.enabled) return
        WelcomeByeGuild.onGuildMemberRemove(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!config.enabled) return
        WelcomeByeGuild.onGuildLeave(event)
    }
}
