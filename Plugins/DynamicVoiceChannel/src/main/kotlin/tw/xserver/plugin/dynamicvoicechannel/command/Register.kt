package tw.xserver.plugin.dynamicvoicechannel.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import tw.xserver.plugin.dynamicvoicechannel.lang.CmdLocalizations

internal val commandStringSet: Set<String> = setOf(
    "dynamic-voice-channel bind",
    "dynamic-voice-channel unbind",
)

internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        Commands.slash("dynamic-voice-channel", "commands about dynamic voice channel")
            .setNameLocalizations(CmdLocalizations.dynamicvc.name)
            .setDescriptionLocalizations(CmdLocalizations.dynamicvc.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addSubcommands(
                SubcommandData("bind", "bind a voice channel")
                    .setNameLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.name)
                    .setDescriptionLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.description)
                    .addOptions(
                        OptionData(OptionType.CHANNEL, "channel", "voice channel to bind", true)
                            .setNameLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.options.channel)
                            .setDescriptionLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.options.channel),
                        OptionData(OptionType.STRING, "format-name-1", "format name 1", true)
                            .setNameLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.options.formatName1)
                            .setDescriptionLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.options.formatName1),
                        OptionData(OptionType.STRING, "format-name-2", "format name 2", true)
                            .setNameLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.options.formatName2)
                            .setDescriptionLocalizations(CmdLocalizations.dynamicvc.subcommands.bind.options.formatName2),
                    ),
                SubcommandData("unbind", "unbind a voice channel")
                    .setNameLocalizations(CmdLocalizations.dynamicvc.subcommands.unbind.name)
                    .setDescriptionLocalizations(CmdLocalizations.dynamicvc.subcommands.unbind.description)
                    .addOptions(
                        OptionData(OptionType.CHANNEL, "channel", "voice channel to unbind", true)
                            .setNameLocalizations(CmdLocalizations.dynamicvc.subcommands.unbind.options.channel)
                            .setDescriptionLocalizations(CmdLocalizations.dynamicvc.subcommands.unbind.options.channel),
                    ),
            )
    )
