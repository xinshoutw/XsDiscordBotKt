package tw.xinshou.plugin.logger.voice.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import tw.xinshou.loader.localizations.StringLocalizer

private object Keys {
    // The key is based on the Kotlin property name 'voiceLogger'.
    const val VOICE_LOGGER = "voiceLogger"
    const val VOICE_LOGGER_NAME = "$VOICE_LOGGER.name"
    const val VOICE_LOGGER_DESC = "$VOICE_LOGGER.description"

    // Subcommand Keys
    private const val VL_SUBCOMMANDS = "$VOICE_LOGGER.subcommands"
    const val VL_SUB_SETTING_NAME = "$VL_SUBCOMMANDS.setting.name"
    const val VL_SUB_SETTING_DESC = "$VL_SUBCOMMANDS.setting.description"
}


internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("voice-logger", "commands about voice logger")
        .setNameLocalizations(localizer.getLocaleData(Keys.VOICE_LOGGER_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.VOICE_LOGGER_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        .addSubcommands(
            SubcommandData("setting", "set voice log in this channel")
                .setNameLocalizations(localizer.getLocaleData(Keys.VL_SUB_SETTING_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.VL_SUB_SETTING_DESC))
        )
)