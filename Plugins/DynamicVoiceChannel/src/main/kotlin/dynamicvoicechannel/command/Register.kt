package tw.xinshou.discord.plugin.dynamicvoicechannel.command

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import tw.xinshou.discord.plugin.dynamicvoicechannel.DynamicVoiceChannel

private object Keys {
    const val BASE = "dynamicvc"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"

    private const val SUBCOMMANDS = "$BASE.subcommands"

    const val SUB_BIND = "$SUBCOMMANDS.bind"
    const val SUB_BIND_NAME = "$SUB_BIND.name"
    const val SUB_BIND_DESC = "$SUB_BIND.description"
    private const val SUB_BIND_OPTIONS = "$SUB_BIND.options"
    const val SUB_BIND_OPT_CHANNEL_NAME = "$SUB_BIND_OPTIONS.channel.name"
    const val SUB_BIND_OPT_CHANNEL_DESC = "$SUB_BIND_OPTIONS.channel.description"
    const val SUB_BIND_OPT_FMT1_NAME = "$SUB_BIND_OPTIONS.formatName1.name"
    const val SUB_BIND_OPT_FMT1_DESC = "$SUB_BIND_OPTIONS.formatName1.description"
    const val SUB_BIND_OPT_FMT2_NAME = "$SUB_BIND_OPTIONS.formatName2.name"
    const val SUB_BIND_OPT_FMT2_DESC = "$SUB_BIND_OPTIONS.formatName2.description"

    const val SUB_UNBIND = "$SUBCOMMANDS.unbind"
    const val SUB_UNBIND_NAME = "$SUB_UNBIND.name"
    const val SUB_UNBIND_DESC = "$SUB_UNBIND.description"
    private const val SUB_UNBIND_OPTIONS = "$SUB_UNBIND.options"
    const val SUB_UNBIND_OPT_CHANNEL_NAME = "$SUB_UNBIND_OPTIONS.channel.name"
    const val SUB_UNBIND_OPT_CHANNEL_DESC = "$SUB_UNBIND_OPTIONS.channel.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("dynamic-voice-channel", "commands about dynamic voice channel")
            .setNameLocalizations(localizer[Keys.NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.DESCRIPTION].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addSubcommands(
                SubcommandData("bind", "bind a voice channel")
                    .setNameLocalizations(localizer[Keys.SUB_BIND_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.SUB_BIND_DESC].toMap())
                    .addOptions(
                        OptionData(OptionType.CHANNEL, "channel", "voice channel to bind", true)
                            .setNameLocalizations(localizer[Keys.SUB_BIND_OPT_CHANNEL_NAME].toMap())
                            .setDescriptionLocalizations(localizer[Keys.SUB_BIND_OPT_CHANNEL_DESC].toMap()),
                        OptionData(OptionType.STRING, "format-name-1", "format name 1", true)
                            .setNameLocalizations(localizer[Keys.SUB_BIND_OPT_FMT1_NAME].toMap())
                            .setDescriptionLocalizations(localizer[Keys.SUB_BIND_OPT_FMT1_DESC].toMap()),
                        OptionData(OptionType.STRING, "format-name-2", "format name 2", true)
                            .setNameLocalizations(localizer[Keys.SUB_BIND_OPT_FMT2_NAME].toMap())
                            .setDescriptionLocalizations(localizer[Keys.SUB_BIND_OPT_FMT2_DESC].toMap()),
                    ),
                SubcommandData("unbind", "unbind a voice channel")
                    .setNameLocalizations(localizer[Keys.SUB_UNBIND_NAME].toMap())
                    .setDescriptionLocalizations(localizer[Keys.SUB_UNBIND_DESC].toMap())
                    .addOptions(
                        OptionData(OptionType.CHANNEL, "channel", "voice channel to unbind", true)
                            .setNameLocalizations(localizer[Keys.SUB_UNBIND_OPT_CHANNEL_NAME].toMap())
                            .setDescriptionLocalizations(localizer[Keys.SUB_UNBIND_OPT_CHANNEL_DESC].toMap()),
                    ),
            ),
    ) { event -> DynamicVoiceChannel.onSlashCommandInteraction(event) }
)
