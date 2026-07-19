package tw.xinshou.discord.plugin.botinfo.command

import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands

private object Keys {
    const val BASE = "botInfo"
    const val NAME = "$BASE.name"
    const val DESCRIPTION = "$BASE.description"
}

internal fun globalCommands(localizer: Localizer): List<CommandData> = listOf(
    Commands.slash("bot-info", "show about the bot data")
        .setNameLocalizations(localizer[Keys.NAME].toMap())
        .setDescriptionLocalizations(localizer[Keys.DESCRIPTION].toMap())
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
)
