package tw.xinshou.discord.plugin.welcomebyeguild.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.discord.core.localizations.StringLocalizer

internal val commandNameSet: Set<String> = setOf(
    "create-welcome-bye-guild",
)

private object Keys {
    const val BASE = "createWelcomeByeGuild"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("create-welcome-bye-guild", "create welcome and leave embeds")
        .setNameLocalizations(localizer.getLocaleData(Keys.NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.DESCRIPTION))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
)
