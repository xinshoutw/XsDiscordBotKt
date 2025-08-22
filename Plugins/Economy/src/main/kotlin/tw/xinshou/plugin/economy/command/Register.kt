package tw.xinshou.plugin.economy.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.core.localizations.StringLocalizer

internal val commandStringSet: Set<String> = setOf(
    "balance",
    "top-money",
    "top-cost",
    "add-money",
    "remove-money",
    "set-money",
    "add-cost",
    "remove-cost",
    "set-cost",
)

private object Keys {
    // Balance Command
    const val BALANCE = "balance"
    const val BALANCE_NAME = "$BALANCE.name"
    const val BALANCE_DESC = "$BALANCE.description"
    const val BALANCE_OPT_MEMBER_NAME = "$BALANCE.options.member.name"
    const val BALANCE_OPT_MEMBER_DESC = "$BALANCE.options.member.description"

    // Top Money Command
    const val TOP_MONEY = "topMoney"
    const val TOP_MONEY_NAME = "$TOP_MONEY.name"
    const val TOP_MONEY_DESC = "$TOP_MONEY.description"

    // Top Cost Command
    const val TOP_COST = "topCost"
    const val TOP_COST_NAME = "$TOP_COST.name"
    const val TOP_COST_DESC = "$TOP_COST.description"

    // Add Money Command
    const val ADD_MONEY = "addMoney"
    const val ADD_MONEY_NAME = "$ADD_MONEY.name"
    const val ADD_MONEY_DESC = "$ADD_MONEY.description"
    const val ADD_MONEY_OPT_MEMBER_NAME = "$ADD_MONEY.options.member.name"
    const val ADD_MONEY_OPT_MEMBER_DESC = "$ADD_MONEY.options.member.description"
    const val ADD_MONEY_OPT_VALUE_NAME = "$ADD_MONEY.options.value.name"
    const val ADD_MONEY_OPT_VALUE_DESC = "$ADD_MONEY.options.value.description"

    // Remove Money Command
    const val REMOVE_MONEY = "removeMoney"
    const val REMOVE_MONEY_NAME = "$REMOVE_MONEY.name"
    const val REMOVE_MONEY_DESC = "$REMOVE_MONEY.description"
    const val REMOVE_MONEY_OPT_MEMBER_NAME = "$REMOVE_MONEY.options.member.name"
    const val REMOVE_MONEY_OPT_MEMBER_DESC = "$REMOVE_MONEY.options.member.description"
    const val REMOVE_MONEY_OPT_VALUE_NAME = "$REMOVE_MONEY.options.value.name"
    const val REMOVE_MONEY_OPT_VALUE_DESC = "$REMOVE_MONEY.options.value.description"

    // Set Money Command
    const val SET_MONEY = "setMoney"
    const val SET_MONEY_NAME = "$SET_MONEY.name"
    const val SET_MONEY_DESC = "$SET_MONEY.description"
    const val SET_MONEY_OPT_MEMBER_NAME = "$SET_MONEY.options.member.name"
    const val SET_MONEY_OPT_MEMBER_DESC = "$SET_MONEY.options.member.description"
    const val SET_MONEY_OPT_VALUE_NAME = "$SET_MONEY.options.value.name"
    const val SET_MONEY_OPT_VALUE_DESC = "$SET_MONEY.options.value.description"

    // Add Cost Command
    const val ADD_COST = "addCost"
    const val ADD_COST_NAME = "$ADD_COST.name"
    const val ADD_COST_DESC = "$ADD_COST.description"
    const val ADD_COST_OPT_MEMBER_NAME = "$ADD_COST.options.member.name"
    const val ADD_COST_OPT_MEMBER_DESC = "$ADD_COST.options.member.description"
    const val ADD_COST_OPT_VALUE_NAME = "$ADD_COST.options.value.name"
    const val ADD_COST_OPT_VALUE_DESC = "$ADD_COST.options.value.description"

    // Remove Cost Command
    const val REMOVE_COST = "removeCost"
    const val REMOVE_COST_NAME = "$REMOVE_COST.name"
    const val REMOVE_COST_DESC = "$REMOVE_COST.description"
    const val REMOVE_COST_OPT_MEMBER_NAME = "$REMOVE_COST.options.member.name"
    const val REMOVE_COST_OPT_MEMBER_DESC = "$REMOVE_COST.options.member.description"
    const val REMOVE_COST_OPT_VALUE_NAME = "$REMOVE_COST.options.value.name"
    const val REMOVE_COST_OPT_VALUE_DESC = "$REMOVE_COST.options.value.description"

    // Set Cost Command
    const val SET_COST = "setCost"
    const val SET_COST_NAME = "$SET_COST.name"
    const val SET_COST_DESC = "$SET_COST.description"
    const val SET_COST_OPT_MEMBER_NAME = "$SET_COST.options.member.name"
    const val SET_COST_OPT_MEMBER_DESC = "$SET_COST.options.member.description"
    const val SET_COST_OPT_VALUE_NAME = "$SET_COST.options.value.name"
    const val SET_COST_OPT_VALUE_DESC = "$SET_COST.options.value.description"
}


internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    // Command to retrieve current money balance of a member.
    Commands.slash("balance", "Get current money from member")
        .setNameLocalizations(localizer.getLocaleData(Keys.BALANCE_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.BALANCE_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member to query.")
                .setNameLocalizations(localizer.getLocaleData(Keys.BALANCE_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.BALANCE_OPT_MEMBER_DESC))
        )
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    // Command to display top money holders.
    Commands.slash("top-money", "Get leaderboard for money")
        .setNameLocalizations(localizer.getLocaleData(Keys.TOP_MONEY_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.TOP_MONEY_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    // Command to display top transaction logs.
    Commands.slash("top-cost", "Get leaderboard from log money")
        .setNameLocalizations(localizer.getLocaleData(Keys.TOP_COST_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.TOP_COST_DESC))
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    // Command to add money to a member's balance.
    Commands.slash("add-money", "Add money to member's balance")
        .setNameLocalizations(localizer.getLocaleData(Keys.ADD_MONEY_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.ADD_MONEY_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.ADD_MONEY_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.ADD_MONEY_OPT_MEMBER_DESC)),
            OptionData(OptionType.INTEGER, "value", "Specify the amount to add.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.ADD_MONEY_OPT_VALUE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.ADD_MONEY_OPT_VALUE_DESC))
        )
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    // Command to remove money from a member's balance.
    Commands.slash("remove-money", "Remove money from member's balance")
        .setNameLocalizations(localizer.getLocaleData(Keys.REMOVE_MONEY_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.REMOVE_MONEY_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.REMOVE_MONEY_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(
                    localizer.getLocaleData(Keys.REMOVE_MONEY_OPT_MEMBER_DESC)
                ),
            OptionData(OptionType.INTEGER, "value", "Specify the amount to remove.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.REMOVE_MONEY_OPT_VALUE_NAME))
                .setDescriptionLocalizations(
                    localizer.getLocaleData(Keys.REMOVE_MONEY_OPT_VALUE_DESC)
                )
        )
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    // Command to set a specific money value to a member's balance.
    Commands.slash("set-money", "Set money to member's balance")
        .setNameLocalizations(localizer.getLocaleData(Keys.SET_MONEY_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.SET_MONEY_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.SET_MONEY_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.SET_MONEY_OPT_MEMBER_DESC)),
            OptionData(OptionType.INTEGER, "value", "Specify the new balance.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.SET_MONEY_OPT_VALUE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.SET_MONEY_OPT_VALUE_DESC))
        )
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    Commands.slash("add-cost", "Add money log to member")
        .setNameLocalizations(localizer.getLocaleData(Keys.ADD_COST_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.ADD_COST_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.ADD_COST_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.ADD_COST_OPT_MEMBER_DESC)),
            OptionData(OptionType.INTEGER, "value", "Specify the amount to add.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.ADD_COST_OPT_VALUE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.ADD_COST_OPT_VALUE_DESC))
        )
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    Commands.slash("remove-cost", "Remove money log to member")
        .setNameLocalizations(localizer.getLocaleData(Keys.REMOVE_COST_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.REMOVE_COST_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.REMOVE_COST_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.REMOVE_COST_OPT_MEMBER_DESC)),
            OptionData(OptionType.INTEGER, "value", "Specify the amount to remove.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.REMOVE_COST_OPT_VALUE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.REMOVE_COST_OPT_VALUE_DESC))
        )
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

    Commands.slash("set-cost", "Set money log to member")
        .setNameLocalizations(localizer.getLocaleData(Keys.SET_COST_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.SET_COST_DESC))
        .addOptions(
            OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.SET_COST_OPT_MEMBER_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.SET_COST_OPT_MEMBER_DESC)),
            OptionData(OptionType.INTEGER, "value", "Specify the new balance.", true)
                .setNameLocalizations(localizer.getLocaleData(Keys.SET_COST_OPT_VALUE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.SET_COST_OPT_VALUE_DESC))
        )
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
)