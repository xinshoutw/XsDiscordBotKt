package tw.xinshou.plugin.rentsystem.command

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.plugin.rentsystem.command.lang.CmdLocalizations

internal val commandStringSet: Set<String> = setOf(
    "meter",
)

internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        Commands.slash("meter", "upload current power meters")
            .setNameLocalizations(CmdLocalizations.meter.name)
            .setDescriptionLocalizations(CmdLocalizations.meter.description)
            .addOptions(
                OptionData(OptionType.STRING, "public", "the power meter value")
                    .setNameLocalizations(CmdLocalizations.meter.options.public.name)
                    .setDescriptionLocalizations(CmdLocalizations.meter.options.public.description)
                    .setRequired(true),
                OptionData(OptionType.STRING, "room-a", "the power meter value of room A")
                    .setNameLocalizations(CmdLocalizations.meter.options.roomA.name)
                    .setDescriptionLocalizations(CmdLocalizations.meter.options.roomA.description)
                    .setRequired(true),
                OptionData(OptionType.STRING, "room-b", "the power meter value of room B")
                    .setNameLocalizations(CmdLocalizations.meter.options.roomB.name)
                    .setDescriptionLocalizations(CmdLocalizations.meter.options.roomB.description)
                    .setRequired(true),
                OptionData(OptionType.STRING, "room-c", "the power meter value of room C")
                    .setNameLocalizations(CmdLocalizations.meter.options.roomC.name)
                    .setDescriptionLocalizations(CmdLocalizations.meter.options.roomC.description)
                    .setRequired(true),
            ).setDefaultPermissions(DefaultMemberPermissions.ENABLED),
    )

