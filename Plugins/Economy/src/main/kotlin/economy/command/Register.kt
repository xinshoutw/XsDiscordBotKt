package tw.xinshou.discord.plugin.economy.command

import core.command.CommandHandler
import core.command.slashCommand
import core.i18n.Localizer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.discord.plugin.economy.Economy

private object Keys {
    const val BALANCE = "balance"
    const val BALANCE_NAME = "$BALANCE.name"
    const val BALANCE_DESC = "$BALANCE.description"
    const val BALANCE_OPT_MEMBER_NAME = "$BALANCE.options.member.name"
    const val BALANCE_OPT_MEMBER_DESC = "$BALANCE.options.member.description"
    const val TOP_MONEY = "topMoney"
    const val TOP_MONEY_NAME = "$TOP_MONEY.name"
    const val TOP_MONEY_DESC = "$TOP_MONEY.description"
    const val TOP_COST = "topCost"
    const val TOP_COST_NAME = "$TOP_COST.name"
    const val TOP_COST_DESC = "$TOP_COST.description"
    const val ADD_MONEY = "addMoney"
    const val ADD_MONEY_NAME = "$ADD_MONEY.name"
    const val ADD_MONEY_DESC = "$ADD_MONEY.description"
    const val ADD_MONEY_OPT_MEMBER_NAME = "$ADD_MONEY.options.member.name"
    const val ADD_MONEY_OPT_MEMBER_DESC = "$ADD_MONEY.options.member.description"
    const val ADD_MONEY_OPT_VALUE_NAME = "$ADD_MONEY.options.value.name"
    const val ADD_MONEY_OPT_VALUE_DESC = "$ADD_MONEY.options.value.description"
    const val REMOVE_MONEY = "removeMoney"
    const val REMOVE_MONEY_NAME = "$REMOVE_MONEY.name"
    const val REMOVE_MONEY_DESC = "$REMOVE_MONEY.description"
    const val REMOVE_MONEY_OPT_MEMBER_NAME = "$REMOVE_MONEY.options.member.name"
    const val REMOVE_MONEY_OPT_MEMBER_DESC = "$REMOVE_MONEY.options.member.description"
    const val REMOVE_MONEY_OPT_VALUE_NAME = "$REMOVE_MONEY.options.value.name"
    const val REMOVE_MONEY_OPT_VALUE_DESC = "$REMOVE_MONEY.options.value.description"
    const val SET_MONEY = "setMoney"
    const val SET_MONEY_NAME = "$SET_MONEY.name"
    const val SET_MONEY_DESC = "$SET_MONEY.description"
    const val SET_MONEY_OPT_MEMBER_NAME = "$SET_MONEY.options.member.name"
    const val SET_MONEY_OPT_MEMBER_DESC = "$SET_MONEY.options.member.description"
    const val SET_MONEY_OPT_VALUE_NAME = "$SET_MONEY.options.value.name"
    const val SET_MONEY_OPT_VALUE_DESC = "$SET_MONEY.options.value.description"
    const val ADD_COST = "addCost"
    const val ADD_COST_NAME = "$ADD_COST.name"
    const val ADD_COST_DESC = "$ADD_COST.description"
    const val ADD_COST_OPT_MEMBER_NAME = "$ADD_COST.options.member.name"
    const val ADD_COST_OPT_MEMBER_DESC = "$ADD_COST.options.member.description"
    const val ADD_COST_OPT_VALUE_NAME = "$ADD_COST.options.value.name"
    const val ADD_COST_OPT_VALUE_DESC = "$ADD_COST.options.value.description"
    const val REMOVE_COST = "removeCost"
    const val REMOVE_COST_NAME = "$REMOVE_COST.name"
    const val REMOVE_COST_DESC = "$REMOVE_COST.description"
    const val REMOVE_COST_OPT_MEMBER_NAME = "$REMOVE_COST.options.member.name"
    const val REMOVE_COST_OPT_MEMBER_DESC = "$REMOVE_COST.options.member.description"
    const val REMOVE_COST_OPT_VALUE_NAME = "$REMOVE_COST.options.value.name"
    const val REMOVE_COST_OPT_VALUE_DESC = "$REMOVE_COST.options.value.description"
    const val SET_COST = "setCost"
    const val SET_COST_NAME = "$SET_COST.name"
    const val SET_COST_DESC = "$SET_COST.description"
    const val SET_COST_OPT_MEMBER_NAME = "$SET_COST.options.member.name"
    const val SET_COST_OPT_MEMBER_DESC = "$SET_COST.options.member.description"
    const val SET_COST_OPT_VALUE_NAME = "$SET_COST.options.value.name"
    const val SET_COST_OPT_VALUE_DESC = "$SET_COST.options.value.description"
}

private fun memberValueCommand(
    name: String, desc: String, localizer: Localizer,
    nameKey: String, descKey: String,
    memberNameKey: String, memberDescKey: String,
    valueNameKey: String, valueDescKey: String,
) = Commands.slash(name, desc)
    .setNameLocalizations(localizer[nameKey].toMap())
    .setDescriptionLocalizations(localizer[descKey].toMap())
    .addOptions(
        OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
            .setNameLocalizations(localizer[memberNameKey].toMap())
            .setDescriptionLocalizations(localizer[memberDescKey].toMap()),
        OptionData(OptionType.INTEGER, "value", "Specify the amount.", true)
            .setNameLocalizations(localizer[valueNameKey].toMap())
            .setDescriptionLocalizations(localizer[valueDescKey].toMap())
    )
    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))

internal fun guildCommands(localizer: Localizer): List<CommandHandler> {
    val handler: suspend (net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent) -> Unit =
        { event -> Economy.onSlashCommandInteraction(event) }

    return listOf(
        slashCommand(
            data = Commands.slash("balance", "Get current money from member")
                .setNameLocalizations(localizer[Keys.BALANCE_NAME].toMap())
                .setDescriptionLocalizations(localizer[Keys.BALANCE_DESC].toMap())
                .addOptions(
                    OptionData(OptionType.USER, "member", "Specify the member to query.")
                        .setNameLocalizations(localizer[Keys.BALANCE_OPT_MEMBER_NAME].toMap())
                        .setDescriptionLocalizations(localizer[Keys.BALANCE_OPT_MEMBER_DESC].toMap())
                )
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
            execute = handler,
        ),
        slashCommand(
            data = Commands.slash("top-money", "Get leaderboard for money")
                .setNameLocalizations(localizer[Keys.TOP_MONEY_NAME].toMap())
                .setDescriptionLocalizations(localizer[Keys.TOP_MONEY_DESC].toMap())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
            execute = handler,
        ),
        slashCommand(
            data = Commands.slash("top-cost", "Get leaderboard from log money")
                .setNameLocalizations(localizer[Keys.TOP_COST_NAME].toMap())
                .setDescriptionLocalizations(localizer[Keys.TOP_COST_DESC].toMap())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
            execute = handler,
        ),
        slashCommand(data = memberValueCommand("add-money", "Add money", localizer, Keys.ADD_MONEY_NAME, Keys.ADD_MONEY_DESC, Keys.ADD_MONEY_OPT_MEMBER_NAME, Keys.ADD_MONEY_OPT_MEMBER_DESC, Keys.ADD_MONEY_OPT_VALUE_NAME, Keys.ADD_MONEY_OPT_VALUE_DESC), execute = handler),
        slashCommand(data = memberValueCommand("remove-money", "Remove money", localizer, Keys.REMOVE_MONEY_NAME, Keys.REMOVE_MONEY_DESC, Keys.REMOVE_MONEY_OPT_MEMBER_NAME, Keys.REMOVE_MONEY_OPT_MEMBER_DESC, Keys.REMOVE_MONEY_OPT_VALUE_NAME, Keys.REMOVE_MONEY_OPT_VALUE_DESC), execute = handler),
        slashCommand(data = memberValueCommand("set-money", "Set money", localizer, Keys.SET_MONEY_NAME, Keys.SET_MONEY_DESC, Keys.SET_MONEY_OPT_MEMBER_NAME, Keys.SET_MONEY_OPT_MEMBER_DESC, Keys.SET_MONEY_OPT_VALUE_NAME, Keys.SET_MONEY_OPT_VALUE_DESC), execute = handler),
        slashCommand(data = memberValueCommand("add-cost", "Add cost", localizer, Keys.ADD_COST_NAME, Keys.ADD_COST_DESC, Keys.ADD_COST_OPT_MEMBER_NAME, Keys.ADD_COST_OPT_MEMBER_DESC, Keys.ADD_COST_OPT_VALUE_NAME, Keys.ADD_COST_OPT_VALUE_DESC), execute = handler),
        slashCommand(data = memberValueCommand("remove-cost", "Remove cost", localizer, Keys.REMOVE_COST_NAME, Keys.REMOVE_COST_DESC, Keys.REMOVE_COST_OPT_MEMBER_NAME, Keys.REMOVE_COST_OPT_MEMBER_DESC, Keys.REMOVE_COST_OPT_VALUE_NAME, Keys.REMOVE_COST_OPT_VALUE_DESC), execute = handler),
        slashCommand(data = memberValueCommand("set-cost", "Set cost", localizer, Keys.SET_COST_NAME, Keys.SET_COST_DESC, Keys.SET_COST_OPT_MEMBER_NAME, Keys.SET_COST_OPT_MEMBER_DESC, Keys.SET_COST_OPT_VALUE_NAME, Keys.SET_COST_OPT_VALUE_DESC), execute = handler),
    )
}
