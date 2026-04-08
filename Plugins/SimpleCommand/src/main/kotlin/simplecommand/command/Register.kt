package tw.xinshou.discord.plugin.simplecommand.command

import tw.xinshou.discord.core.command.CommandHandler
import tw.xinshou.discord.core.command.slashCommand
import tw.xinshou.discord.core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.discord.plugin.simplecommand.SimpleCommand

private object Keys {
    const val CUB_NONE_CARD_NAME = "cubNoneCard.name"
    const val CUB_NONE_CARD_DESC = "cubNoneCard.description"

    const val TSIB_NONE_CARD_NAME = "tsibNoneCard.name"
    const val TSIB_NONE_CARD_DESC = "tsibNoneCard.description"

    const val CTCB_NONE_CARD_NAME = "ctcbNoneCard.name"
    const val CTCB_NONE_CARD_DESC = "ctcbNoneCard.description"

    const val CTCB_REMIT_NAME = "ctcbRemit.name"
    const val CTCB_REMIT_DESC = "ctcbRemit.description"

    const val CHPYTWTP_REMIT_NAME = "chpytwtpRemit.name"
    const val CHPYTWTP_REMIT_DESC = "chpytwtpRemit.description"
}

internal fun guildCommands(localizer: Localizer): List<CommandHandler> = listOf(
    slashCommand(
        data = Commands.slash("cub-none-card", "generate message")
            .setNameLocalizations(localizer[Keys.CUB_NONE_CARD_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.CUB_NONE_CARD_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    ) { event -> SimpleCommand.onSlashCommandInteraction(event) },
    slashCommand(
        data = Commands.slash("tsib-none-card", "generate message")
            .setNameLocalizations(localizer[Keys.TSIB_NONE_CARD_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.TSIB_NONE_CARD_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    ) { event -> SimpleCommand.onSlashCommandInteraction(event) },
    slashCommand(
        data = Commands.slash("ctcb-none-card", "generate message")
            .setNameLocalizations(localizer[Keys.CTCB_NONE_CARD_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.CTCB_NONE_CARD_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    ) { event -> SimpleCommand.onSlashCommandInteraction(event) },
    slashCommand(
        data = Commands.slash("ctcb-remit", "generate message")
            .setNameLocalizations(localizer[Keys.CTCB_REMIT_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.CTCB_REMIT_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    ) { event -> SimpleCommand.onSlashCommandInteraction(event) },
    slashCommand(
        data = Commands.slash("chpytwtp-remit", "generate message")
            .setNameLocalizations(localizer[Keys.CHPYTWTP_REMIT_NAME].toMap())
            .setDescriptionLocalizations(localizer[Keys.CHPYTWTP_REMIT_DESC].toMap())
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    ) { event -> SimpleCommand.onSlashCommandInteraction(event) },
)
