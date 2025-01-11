package tw.xserver.plugin.basiccalculator.command

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xserver.plugin.basiccalculator.lang.CmdLocalizations

internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        Commands.slash("basic-calculate", "calculate + - * / ^ ( ) math problem")
            .setNameLocalizations(CmdLocalizations.basicCalculate.name)
            .setDescriptionLocalizations(CmdLocalizations.basicCalculate.description)
            .addOptions(
                OptionData(OptionType.STRING, "formula", "What's problem?", true)
                    .setNameLocalizations(CmdLocalizations.basicCalculate.options.formula.name)
                    .setDescriptionLocalizations(CmdLocalizations.basicCalculate.options.formula.description)
            )
    )
