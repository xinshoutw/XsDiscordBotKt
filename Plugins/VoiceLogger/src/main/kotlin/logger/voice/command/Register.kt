package tw.xinshou.discord.plugin.logger.voice.command

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import tw.xinshou.discord.plugin.logger.voice.VoiceLogger

private object Keys {
    const val VOICE_LOGGER = "voiceLogger"
    const val VOICE_LOGGER_NAME = "$VOICE_LOGGER.name"
    const val VOICE_LOGGER_DESC = "$VOICE_LOGGER.description"

    private const val VL_SUBCOMMANDS = "$VOICE_LOGGER.subcommands"
    const val VL_SUB_SETTING_NAME = "$VL_SUBCOMMANDS.setting.name"
    const val VL_SUB_SETTING_DESC = "$VL_SUBCOMMANDS.setting.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("voice-logger", "commands about voice logger")
            .setNameLocalizations(localizer[Keys.VOICE_LOGGER_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.VOICE_LOGGER_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addSubcommands(
                SubcommandData("setting", "set voice log in this channel")
                    .setNameLocalizations(localizer[Keys.VL_SUB_SETTING_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.VL_SUB_SETTING_DESC].toMap())
            ),
    ) { event -> VoiceLogger.onSlashCommandInteraction(event) }
)
