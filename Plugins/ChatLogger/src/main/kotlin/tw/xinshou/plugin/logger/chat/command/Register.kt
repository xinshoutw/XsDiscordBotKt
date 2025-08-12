package tw.xinshou.plugin.logger.chat.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import tw.xinshou.loader.localizations.StringLocalizer


private object Keys {
    const val BASE = "chat-logger"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"

    private const val SUBCOMMANDS = "$BASE.subcommands"
    const val SUB_SETTING = "$SUBCOMMANDS.setting"
    const val SUB_SETTING_NAME = "$SUB_SETTING.name"
    const val SUB_SETTING_DESC = "$SUB_SETTING.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("basic-calculate", "calculate + - * / ^ ( ) math problem")
        .setNameLocalizations(localizer.getLocaleData(Keys.NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.DESCRIPTION))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))

        .addSubcommands(
            SubcommandData("setting", "set chat log in this channel")
                .setNameLocalizations(localizer.getLocaleData(Keys.SUB_SETTING_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.SUB_SETTING_DESC))
        )
)
