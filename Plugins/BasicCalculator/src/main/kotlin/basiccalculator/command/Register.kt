package tw.xinshou.discord.plugin.basiccalculator.command

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.core.localizations.StringLocalizer

private object Keys {
    const val BASE = "basicCalculate"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"
    const val OPT_FORMULA = "$BASE.options.formula"
    const val OPT_FORMULA_NAME = "$OPT_FORMULA.name"
    const val OPT_FORMULA_DESC = "$OPT_FORMULA.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("basic-calculate", "calculate + - * / ^ ( ) math problem")
        .setNameLocalizations(localizer.getLocaleData(Keys.NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.DESCRIPTION))
        .addOptions(
            OptionData(OptionType.STRING, "formula", "What's problem?", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.OPT_FORMULA_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.OPT_FORMULA_DESC))
        )
)