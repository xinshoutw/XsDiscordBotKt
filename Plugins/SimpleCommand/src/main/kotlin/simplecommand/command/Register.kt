package tw.xinshou.discord.plugin.simplecommand.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import tw.xinshou.discord.core.localizations.StringLocalizer

internal val commandStringSet: Set<String> = setOf(
    "cub-none-card",
    "tsib-none-card",
    "ctcb-none-card",
    "ctcb-remit",
    "chpytwtp-remit"
)

private object Keys {
    const val CUB_NONE_CARD = "cubNoneCard"
    const val CUB_NONE_CARD_NAME = "$CUB_NONE_CARD.name"
    const val CUB_NONE_CARD_DESC = "$CUB_NONE_CARD.description"

    const val TSIB_NONE_CARD = "tsibNoneCard"
    const val TSIB_NONE_CARD_NAME = "$TSIB_NONE_CARD.name"
    const val TSIB_NONE_CARD_DESC = "$TSIB_NONE_CARD.description"

    const val CTCB_NONE_CARD = "ctcbNoneCard"
    const val CTCB_NONE_CARD_NAME = "$CTCB_NONE_CARD.name"
    const val CTCB_NONE_CARD_DESC = "$CTCB_NONE_CARD.description"

    const val CTCB_REMIT = "ctcbRemit"
    const val CTCB_REMIT_NAME = "$CTCB_REMIT.name"
    const val CTCB_REMIT_DESC = "$CTCB_REMIT.description"

    const val CHPYTWTP_REMIT = "chpytwtpRemit"
    const val CHPYTWTP_REMIT_NAME = "$CHPYTWTP_REMIT.name"
    const val CHPYTWTP_REMIT_DESC = "$CHPYTWTP_REMIT.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    Commands.slash("cub-none-card", "generate message")
        .setNameLocalizations(localizer.getLocaleData(Keys.CUB_NONE_CARD_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.CUB_NONE_CARD_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    Commands.slash("tsib-none-card", "generate message")
        .setNameLocalizations(localizer.getLocaleData(Keys.TSIB_NONE_CARD_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.TSIB_NONE_CARD_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    Commands.slash("ctcb-none-card", "generate message")
        .setNameLocalizations(localizer.getLocaleData(Keys.CTCB_NONE_CARD_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.CTCB_NONE_CARD_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    Commands.slash("ctcb-remit", "generate message")
        .setNameLocalizations(localizer.getLocaleData(Keys.CTCB_REMIT_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.CTCB_REMIT_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    Commands.slash("chpytwtp-remit", "generate message")
        .setNameLocalizations(localizer.getLocaleData(Keys.CHPYTWTP_REMIT_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.CHPYTWTP_REMIT_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
)