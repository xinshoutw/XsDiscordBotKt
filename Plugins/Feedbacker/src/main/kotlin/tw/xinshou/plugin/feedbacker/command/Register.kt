package tw.xinshou.plugin.feedbacker.command

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.core.localizations.StringLocalizer

private object Keys {
    const val BASE = "feedbacker"
    const val FEEDBACKER_NAME = "$BASE.name"
    const val FEEDBACKER_DESC = "$BASE.description"

    private const val FEEDBACKER_OPTIONS = "$BASE.options"
    const val FEEDBACKER_OPT_MEMBER_NAME = "$FEEDBACKER_OPTIONS.member.name"
    const val FEEDBACKER_OPT_MEMBER_DESC = "$FEEDBACKER_OPTIONS.member.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("feedbacker", "Create a feedback form for a member")
        .setNameLocalizations(localizer.getLocaleData(Keys.FEEDBACKER_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.FEEDBACKER_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.FEEDBACKER_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.FEEDBACKER_OPT_MEMBER_DESC))
        ),
)
