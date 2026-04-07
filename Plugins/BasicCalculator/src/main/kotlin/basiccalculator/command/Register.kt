package tw.xinshou.discord.plugin.basiccalculator.command

import core.i18n.Localizer
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData

private object Keys {
    const val BASE = "basicCalculate"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"
    const val OPT_FORMULA = "$BASE.options.formula"
    const val OPT_FORMULA_NAME = "$OPT_FORMULA.name"
    const val OPT_FORMULA_DESC = "$OPT_FORMULA.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandData> = listOf(
    Commands.slash("basic-calculate", "calculate + - * / ^ ( ) math problem")
        .setNameLocalizations(localizer[Keys.NAME].toMap())
        .setDescriptionLocalizations(localizer[Keys.DESCRIPTION].toMap())
        .addOptions(
            OptionData(OptionType.STRING, "formula", "What's problem?", true)
                .setNameLocalizations(localizer[Keys.OPT_FORMULA_NAME].toMap())
                .setDescriptionLocalizations(localizer[Keys.OPT_FORMULA_DESC].toMap())
        )
)
