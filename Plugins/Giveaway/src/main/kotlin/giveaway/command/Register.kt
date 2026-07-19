package tw.xinshou.discord.plugin.giveaway.command

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.discord.plugin.giveaway.Giveaway

private object Keys {
    const val CREATE_GIVEAWAY = "createGiveaway"
    const val CREATE_GIVEAWAY_NAME = "$CREATE_GIVEAWAY.name"
    const val CREATE_GIVEAWAY_DESCRIPTION = "$CREATE_GIVEAWAY.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("create-giveaway", "Create a giveaway with step-by-step setup")
            .setNameLocalizations(localizer[Keys.CREATE_GIVEAWAY_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.CREATE_GIVEAWAY_DESCRIPTION].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    ) { event -> Giveaway.onSlashCommandInteraction(event) }
)
