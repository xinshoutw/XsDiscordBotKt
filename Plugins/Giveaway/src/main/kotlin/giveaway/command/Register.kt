package tw.xinshou.discord.plugin.giveaway.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.discord.core.localizations.StringLocalizer

internal val commandNameSet: Set<String> = setOf(
    "create-giveaway",
)

private object Keys {
    const val CREATE_GIVEAWAY = "createGiveaway"
    const val CREATE_GIVEAWAY_NAME = "$CREATE_GIVEAWAY.name"
    const val CREATE_GIVEAWAY_DESCRIPTION = "$CREATE_GIVEAWAY.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("create-giveaway", "Create a giveaway with step-by-step setup")
        .setNameLocalizations(localizer.getLocaleData(Keys.CREATE_GIVEAWAY_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.CREATE_GIVEAWAY_DESCRIPTION))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
)
