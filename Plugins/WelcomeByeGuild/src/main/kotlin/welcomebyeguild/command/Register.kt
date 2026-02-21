package tw.xinshou.discord.plugin.welcomebyeguild.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.core.localizations.StringLocalizer

internal val commandNameSet: Set<String> = setOf(
    "welcome-channel-bind",
    "welcome-channel-unbind",
    "bye-channel-bind",
    "bye-channel-unbind",
)

private object Keys {
    const val WELCOME_BIND = "welcomeChannelBind"
    const val WELCOME_BIND_NAME = "$WELCOME_BIND.name"
    const val WELCOME_BIND_DESC = "$WELCOME_BIND.description"
    const val WELCOME_BIND_OPT_CHANNEL_NAME = "$WELCOME_BIND.options.channel.name"
    const val WELCOME_BIND_OPT_CHANNEL_DESC = "$WELCOME_BIND.options.channel.description"

    const val WELCOME_UNBIND = "welcomeChannelUnbind"
    const val WELCOME_UNBIND_NAME = "$WELCOME_UNBIND.name"
    const val WELCOME_UNBIND_DESC = "$WELCOME_UNBIND.description"
    const val WELCOME_UNBIND_OPT_CHANNEL_NAME = "$WELCOME_UNBIND.options.channel.name"
    const val WELCOME_UNBIND_OPT_CHANNEL_DESC = "$WELCOME_UNBIND.options.channel.description"

    const val BYE_BIND = "byeChannelBind"
    const val BYE_BIND_NAME = "$BYE_BIND.name"
    const val BYE_BIND_DESC = "$BYE_BIND.description"
    const val BYE_BIND_OPT_CHANNEL_NAME = "$BYE_BIND.options.channel.name"
    const val BYE_BIND_OPT_CHANNEL_DESC = "$BYE_BIND.options.channel.description"

    const val BYE_UNBIND = "byeChannelUnbind"
    const val BYE_UNBIND_NAME = "$BYE_UNBIND.name"
    const val BYE_UNBIND_DESC = "$BYE_UNBIND.description"
    const val BYE_UNBIND_OPT_CHANNEL_NAME = "$BYE_UNBIND.options.channel.name"
    const val BYE_UNBIND_OPT_CHANNEL_DESC = "$BYE_UNBIND.options.channel.description"
}

private val adminPermission = DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("welcome-channel-bind", "bind channel for welcome message")
        .setNameLocalizations(localizer.getLocaleData(Keys.WELCOME_BIND_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.WELCOME_BIND_DESC))
        .setDefaultPermissions(adminPermission)
        .addOptions(
            OptionData(OptionType.CHANNEL, "channel", "welcome output channel", false)
                .setChannelTypes(ChannelType.TEXT)
                .setNameLocalizations(localizer.getLocaleData(Keys.WELCOME_BIND_OPT_CHANNEL_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.WELCOME_BIND_OPT_CHANNEL_DESC))
        ),

    Commands.slash("welcome-channel-unbind", "unbind welcome channel")
        .setNameLocalizations(localizer.getLocaleData(Keys.WELCOME_UNBIND_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.WELCOME_UNBIND_DESC))
        .setDefaultPermissions(adminPermission)
        .addOptions(
            OptionData(OptionType.CHANNEL, "channel", "welcome output channel", false)
                .setChannelTypes(ChannelType.TEXT)
                .setNameLocalizations(localizer.getLocaleData(Keys.WELCOME_UNBIND_OPT_CHANNEL_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.WELCOME_UNBIND_OPT_CHANNEL_DESC))
        ),

    Commands.slash("bye-channel-bind", "bind channel for bye message")
        .setNameLocalizations(localizer.getLocaleData(Keys.BYE_BIND_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.BYE_BIND_DESC))
        .setDefaultPermissions(adminPermission)
        .addOptions(
            OptionData(OptionType.CHANNEL, "channel", "bye output channel", false)
                .setChannelTypes(ChannelType.TEXT)
                .setNameLocalizations(localizer.getLocaleData(Keys.BYE_BIND_OPT_CHANNEL_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.BYE_BIND_OPT_CHANNEL_DESC))
        ),

    Commands.slash("bye-channel-unbind", "unbind bye channel")
        .setNameLocalizations(localizer.getLocaleData(Keys.BYE_UNBIND_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.BYE_UNBIND_DESC))
        .setDefaultPermissions(adminPermission)
        .addOptions(
            OptionData(OptionType.CHANNEL, "channel", "bye output channel", false)
                .setChannelTypes(ChannelType.TEXT)
                .setNameLocalizations(localizer.getLocaleData(Keys.BYE_UNBIND_OPT_CHANNEL_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.BYE_UNBIND_OPT_CHANNEL_DESC))
        )
)
