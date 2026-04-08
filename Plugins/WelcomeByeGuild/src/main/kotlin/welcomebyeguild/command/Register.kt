package tw.xinshou.discord.plugin.welcomebyeguild.command

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.plugin.welcomebyeguild.WelcomeByeGuild

private object Keys {
    const val WELCOME_BIND_NAME = "welcomeChannelBind.name"
    const val WELCOME_BIND_DESC = "welcomeChannelBind.description"
    const val WELCOME_BIND_OPT_CHANNEL_NAME = "welcomeChannelBind.options.channel.name"
    const val WELCOME_BIND_OPT_CHANNEL_DESC = "welcomeChannelBind.options.channel.description"

    const val WELCOME_UNBIND_NAME = "welcomeChannelUnbind.name"
    const val WELCOME_UNBIND_DESC = "welcomeChannelUnbind.description"

    const val BYE_BIND_NAME = "byeChannelBind.name"
    const val BYE_BIND_DESC = "byeChannelBind.description"
    const val BYE_BIND_OPT_CHANNEL_NAME = "byeChannelBind.options.channel.name"
    const val BYE_BIND_OPT_CHANNEL_DESC = "byeChannelBind.options.channel.description"

    const val BYE_UNBIND_NAME = "byeChannelUnbind.name"
    const val BYE_UNBIND_DESC = "byeChannelUnbind.description"
}

private val adminPermission = DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("welcome-channel-bind", "bind channel for welcome message")
            .setNameLocalizations(localizer[Keys.WELCOME_BIND_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.WELCOME_BIND_DESC].toMap())
            .setDefaultPermissions(adminPermission)
            .addOptions(
                OptionData(OptionType.CHANNEL, "channel", "welcome output channel", false)
                    .setChannelTypes(ChannelType.TEXT)
                    .setNameLocalizations(localizer[Keys.WELCOME_BIND_OPT_CHANNEL_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.WELCOME_BIND_OPT_CHANNEL_DESC].toMap())
            ),
    ) { event -> WelcomeByeGuild.onSlashCommandInteraction(event) },

    slashCommand(
        data = Commands.slash("welcome-channel-unbind", "unbind welcome channel")
            .setNameLocalizations(localizer[Keys.WELCOME_UNBIND_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.WELCOME_UNBIND_DESC].toMap())
            .setDefaultPermissions(adminPermission),
    ) { event -> WelcomeByeGuild.onSlashCommandInteraction(event) },

    slashCommand(
        data = Commands.slash("bye-channel-bind", "bind channel for bye message")
            .setNameLocalizations(localizer[Keys.BYE_BIND_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.BYE_BIND_DESC].toMap())
            .setDefaultPermissions(adminPermission)
            .addOptions(
                OptionData(OptionType.CHANNEL, "channel", "bye output channel", false)
                    .setChannelTypes(ChannelType.TEXT)
                    .setNameLocalizations(localizer[Keys.BYE_BIND_OPT_CHANNEL_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.BYE_BIND_OPT_CHANNEL_DESC].toMap())
            ),
    ) { event -> WelcomeByeGuild.onSlashCommandInteraction(event) },

    slashCommand(
        data = Commands.slash("bye-channel-unbind", "unbind bye channel")
            .setNameLocalizations(localizer[Keys.BYE_UNBIND_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.BYE_UNBIND_DESC].toMap())
            .setDefaultPermissions(adminPermission),
    ) { event -> WelcomeByeGuild.onSlashCommandInteraction(event) },
)
