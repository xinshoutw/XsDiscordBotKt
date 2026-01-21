package tw.xinshou.discord.plugin._example.command

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.core.localizations.StringLocalizer

private object Keys {
    const val BASE = "example"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"
    const val OPT1 = "$BASE.options.option1"
    const val OPT1_NAME = "$OPT1.name"
    const val OPT1_DESC = "$OPT1.description"
    const val OPT2 = "$BASE.options.option2"
    const val OPT2_NAME = "$OPT2.name"
    const val OPT2_DESC = "$OPT2.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("example-command", "this is an example command")
        .setNameLocalizations(localizer.getLocaleData(Keys.NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.DESCRIPTION))
        .addOptions(
            OptionData(OptionType.STRING, "option1", "this is an example option", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.OPT1_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.OPT1_DESC)),
            OptionData(OptionType.INTEGER, "option2", "this is an example option", false)
                .setNameLocalizations(localizer.getLocaleData(Keys.OPT2_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.OPT2_DESC))
        )
)