package tw.xinshou.plugin.economy.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.plugin.economy.lang.CmdLocalizations

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

/**
 * Retrieves and constructs an array of guild-specific command configurations.
 * Each command is tailored for economic transactions like checking balance, modifying balances, and viewing leaderboards.
 *
 * @return Array<CommandData> Collection of guild commands configured with localizations and permissions.
 */
internal val guildCommands: Array<CommandData>
    get() = arrayOf(
        // Command to retrieve current money balance of a member.
        Commands.slash("balance", "Get current money from member")
            .setNameLocalizations(CmdLocalizations.balance.name)
            .setDescriptionLocalizations(CmdLocalizations.balance.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member to query.")
                    .setNameLocalizations(CmdLocalizations.balance.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.balance.options.member.description)
            )
            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

        // Command to display top money holders.
        Commands.slash("top-money", "Get leaderboard for money")
            .setNameLocalizations(CmdLocalizations.topMoney.name)
            .setDescriptionLocalizations(CmdLocalizations.topMoney.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        // Command to display top transaction logs.
        Commands.slash("top-cost", "Get leaderboard from log money")
            .setNameLocalizations(CmdLocalizations.topCost.name)
            .setDescriptionLocalizations(CmdLocalizations.topCost.description)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        // Command to add money to a member's balance.
        Commands.slash("add-money", "Add money to member's balance")
            .setNameLocalizations(CmdLocalizations.addMoney.name)
            .setDescriptionLocalizations(CmdLocalizations.addMoney.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                    .setNameLocalizations(CmdLocalizations.addMoney.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.addMoney.options.member.description),
                OptionData(OptionType.INTEGER, "value", "Specify the amount to add.", true)
                    .setNameLocalizations(CmdLocalizations.addMoney.options.value.name)
                    .setDescriptionLocalizations(CmdLocalizations.addMoney.options.value.description)
            )
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        // Command to remove money from a member's balance.
        Commands.slash("remove-money", "Remove money from member's balance")
            .setNameLocalizations(CmdLocalizations.removeMoney.name)
            .setDescriptionLocalizations(CmdLocalizations.removeMoney.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                    .setNameLocalizations(CmdLocalizations.removeMoney.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.removeMoney.options.member.description),
                OptionData(OptionType.INTEGER, "value", "Specify the amount to remove.", true)
                    .setNameLocalizations(CmdLocalizations.removeMoney.options.value.name)
                    .setDescriptionLocalizations(CmdLocalizations.removeMoney.options.value.description)
            )
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        // Command to set a specific money value to a member's balance.
        Commands.slash("set-money", "Set money to member's balance")
            .setNameLocalizations(CmdLocalizations.setMoney.name)
            .setDescriptionLocalizations(CmdLocalizations.setMoney.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                    .setNameLocalizations(CmdLocalizations.setMoney.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.setMoney.options.member.description),
                OptionData(OptionType.INTEGER, "value", "Specify the new balance.", true)
                    .setNameLocalizations(CmdLocalizations.setMoney.options.value.name)
                    .setDescriptionLocalizations(CmdLocalizations.setMoney.options.value.description)
            )
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        // Command to record a transaction log adjustment for a member.
        Commands.slash("add-cost", "Add money log to member")
            .setNameLocalizations(CmdLocalizations.addCost.name)
            .setDescriptionLocalizations(CmdLocalizations.addCost.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                    .setNameLocalizations(CmdLocalizations.addCost.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.addCost.options.member.description),
                OptionData(OptionType.INTEGER, "value", "Specify the amount to add.", true)
                    .setNameLocalizations(CmdLocalizations.addCost.options.value.name)
                    .setDescriptionLocalizations(CmdLocalizations.addCost.options.value.description)
            )
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        Commands.slash("remove-cost", "Remove money log to member")
            .setNameLocalizations(CmdLocalizations.removeCost.name)
            .setDescriptionLocalizations(CmdLocalizations.removeCost.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                    .setNameLocalizations(CmdLocalizations.removeCost.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.removeCost.options.member.description),
                OptionData(OptionType.INTEGER, "value", "Specify the amount to remove.", true)
                    .setNameLocalizations(CmdLocalizations.removeCost.options.value.name)
                    .setDescriptionLocalizations(CmdLocalizations.removeCost.options.value.description)
            )
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),

        Commands.slash("set-cost", "Set money log to member")
            .setNameLocalizations(CmdLocalizations.setCost.name)
            .setDescriptionLocalizations(CmdLocalizations.setCost.description)
            .addOptions(
                OptionData(OptionType.USER, "member", "Specify the member to modify.", true)
                    .setNameLocalizations(CmdLocalizations.setCost.options.member.name)
                    .setDescriptionLocalizations(CmdLocalizations.setCost.options.member.description),
                OptionData(OptionType.INTEGER, "value", "Specify the new balance.", true)
                    .setNameLocalizations(CmdLocalizations.setCost.options.value.name)
                    .setDescriptionLocalizations(CmdLocalizations.setCost.options.value.description)
            )
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
    )
