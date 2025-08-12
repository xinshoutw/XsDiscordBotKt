package tw.xinshou.plugin.botinfo.command

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.loader.localizations.StringLocalizer

private object Keys {
    const val BASE = "bot-info"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"
}

internal fun globalCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("bot-info", "show about the bot data")
        .setNameLocalizations(localizer.getLocaleData(Keys.NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.DESCRIPTION))
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
)
