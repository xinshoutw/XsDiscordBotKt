package tw.xinshou.discord.plugin.logger.chat.command

import core.command.CommandHandler
import core.command.slashCommand
import core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import tw.xinshou.discord.plugin.logger.chat.ChatLogger

private object Keys {
    const val BASE = "chatLogger"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"

    private const val SUBCOMMANDS = "$BASE.subcommands"
    const val SUB_SETTING = "$SUBCOMMANDS.setting"
    const val SUB_SETTING_NAME = "$SUB_SETTING.name"
    const val SUB_SETTING_DESC = "$SUB_SETTING.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("chat-logger", "commands about chat logger")
            .setNameLocalizations(localizer[Keys.NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.DESCRIPTION].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addSubcommands(
                SubcommandData("setting", "set chat log in this channel")
                    .setNameLocalizations(localizer[Keys.SUB_SETTING_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.SUB_SETTING_DESC].toMap())
            ),
    ) { event -> ChatLogger.onSlashCommandInteraction(event) }
)
