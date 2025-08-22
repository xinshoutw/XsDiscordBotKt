package tw.xinshou.plugin.rentsystem.command

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tw.xinshou.core.localizations.StringLocalizer

internal val commandStringSet: Set<String> = setOf(
    "daily-electricity",
    "electricity-bill",
    "water-bill",
    "rent-overview"
)

private object Keys {
    // Daily Electricity Command
    const val DAILY_ELECTRICITY = "dailyElectricity"
    const val DAILY_ELECTRICITY_NAME = "$DAILY_ELECTRICITY.name"
    const val DAILY_ELECTRICITY_DESC = "$DAILY_ELECTRICITY.description"
    private const val DE_OPTIONS = "$DAILY_ELECTRICITY.options"
    const val DE_OPT_PUBLIC_NAME = "$DE_OPTIONS.public.name"
    const val DE_OPT_PUBLIC_DESC = "$DE_OPTIONS.public.description"
    const val DE_OPT_ROOM_A_NAME = "$DE_OPTIONS.roomA.name"
    const val DE_OPT_ROOM_A_DESC = "$DE_OPTIONS.roomA.description"
    const val DE_OPT_ROOM_B_NAME = "$DE_OPTIONS.roomB.name"
    const val DE_OPT_ROOM_B_DESC = "$DE_OPTIONS.roomB.description"
    const val DE_OPT_ROOM_C_NAME = "$DE_OPTIONS.roomC.name"
    const val DE_OPT_ROOM_C_DESC = "$DE_OPTIONS.roomC.description"
    const val DE_OPT_DATE_NAME = "$DE_OPTIONS.date.name"
    const val DE_OPT_DATE_DESC = "$DE_OPTIONS.date.description"

    // Electricity Bill Command
    const val ELECTRICITY_BILL = "electricity-bill"
    const val ELECTRICITY_BILL_NAME = "$ELECTRICITY_BILL.name"
    const val ELECTRICITY_BILL_DESC = "$ELECTRICITY_BILL.description"
    private const val EB_OPTIONS = "$ELECTRICITY_BILL.options"
    const val EB_OPT_START_NAME = "$EB_OPTIONS.period-start.name"
    const val EB_OPT_START_DESC = "$EB_OPTIONS.period-start.description"
    const val EB_OPT_END_NAME = "$EB_OPTIONS.period-end.name"
    const val EB_OPT_END_DESC = "$EB_OPTIONS.period-end.description"
    const val EB_OPT_TOTAL_USAGE_NAME = "$EB_OPTIONS.total-usage.name"
    const val EB_OPT_TOTAL_USAGE_DESC = "$EB_OPTIONS.total-usage.description"
    const val EB_OPT_TOTAL_AMOUNT_NAME = "$EB_OPTIONS.total-amount.name"
    const val EB_OPT_TOTAL_AMOUNT_DESC = "$EB_OPTIONS.total-amount.description"
    const val EB_OPT_PUBLIC_USAGE_NAME = "$EB_OPTIONS.public-usage.name"
    const val EB_OPT_PUBLIC_USAGE_DESC = "$EB_OPTIONS.public-usage.description"
    const val EB_OPT_ROOM_A_USAGE_NAME = "$EB_OPTIONS.room-a-usage.name"
    const val EB_OPT_ROOM_A_USAGE_DESC = "$EB_OPTIONS.room-a-usage.description"
    const val EB_OPT_ROOM_B_USAGE_NAME = "$EB_OPTIONS.room-b-usage.name"
    const val EB_OPT_ROOM_B_USAGE_DESC = "$EB_OPTIONS.room-b-usage.description"
    const val EB_OPT_ROOM_C_USAGE_NAME = "$EB_OPTIONS.room-c-usage.name"
    const val EB_OPT_ROOM_C_USAGE_DESC = "$EB_OPTIONS.room-c-usage.description"

    // Water Bill Command
    const val WATER_BILL = "water-bill"
    const val WATER_BILL_NAME = "$WATER_BILL.name"
    const val WATER_BILL_DESC = "$WATER_BILL.description"
    private const val WB_OPTIONS = "$WATER_BILL.options"
    const val WB_OPT_MONTH_NAME = "$WB_OPTIONS.bill-month.name"
    const val WB_OPT_MONTH_DESC = "$WB_OPTIONS.bill-month.description"
    const val WB_OPT_TOTAL_USAGE_NAME = "$WB_OPTIONS.total-usage.name"
    const val WB_OPT_TOTAL_USAGE_DESC = "$WB_OPTIONS.total-usage.description"
    const val WB_OPT_TOTAL_AMOUNT_NAME = "$WB_OPTIONS.total-amount.name"
    const val WB_OPT_TOTAL_AMOUNT_DESC = "$WB_OPTIONS.total-amount.description"

    // Rent Overview Command
    const val RENT_OVERVIEW = "rent-overview"
    const val RENT_OVERVIEW_NAME = "$RENT_OVERVIEW.name"
    const val RENT_OVERVIEW_DESC = "$RENT_OVERVIEW.description"
    private const val RO_OPTIONS = "$RENT_OVERVIEW.options"
    const val RO_OPT_MONTH_NAME = "$RO_OPTIONS.month.name"
    const val RO_OPT_MONTH_DESC = "$RO_OPTIONS.month.description"
}

internal fun guildCommands(localizer: StringLocalizer<CmdFileSerializer>): Array<CommandData> = arrayOf(
    // 每日電表讀數記錄指令
    Commands.slash("daily-electricity", "Record daily electricity meter readings")
        .setNameLocalizations(localizer.getLocaleData(Keys.DAILY_ELECTRICITY_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.DAILY_ELECTRICITY_DESC))
        .addOptions(
            OptionData(OptionType.NUMBER, "public", "Public area electricity meter reading (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.DE_OPT_PUBLIC_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.DE_OPT_PUBLIC_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "room-a", "Room A electricity meter reading (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.DE_OPT_ROOM_A_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.DE_OPT_ROOM_A_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "room-b", "Room B electricity meter reading (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.DE_OPT_ROOM_B_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.DE_OPT_ROOM_B_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "room-c", "Room C electricity meter reading (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.DE_OPT_ROOM_C_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.DE_OPT_ROOM_C_DESC))
                .setRequired(true),
            OptionData(OptionType.STRING, "date", "Record date (YYYY-MM-DD format, optional - defaults to today)")
                .setNameLocalizations(localizer.getLocaleData(Keys.DE_OPT_DATE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.DE_OPT_DATE_DESC))
                .setRequired(false)
        ).setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    // 兩個月一次的電費帳單記錄指令
    Commands.slash("electricity-bill", "Record bi-monthly electricity bill")
        .setNameLocalizations(localizer.getLocaleData(Keys.ELECTRICITY_BILL_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.ELECTRICITY_BILL_DESC))
        .addOptions(
            OptionData(OptionType.STRING, "period-start", "Bill period start date (YYYY-MM-DD)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_START_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_START_DESC))
                .setRequired(true),
            OptionData(OptionType.STRING, "period-end", "Bill period end date (YYYY-MM-DD)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_END_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_END_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "total-usage", "Total electricity usage (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_TOTAL_USAGE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_TOTAL_USAGE_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "total-amount", "Total bill amount (including fees and discounts)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_TOTAL_AMOUNT_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_TOTAL_AMOUNT_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "public-usage", "Public area usage (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_PUBLIC_USAGE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_PUBLIC_USAGE_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "room-a-usage", "Room A usage (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_ROOM_A_USAGE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_ROOM_A_USAGE_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "room-b-usage", "Room B usage (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_ROOM_B_USAGE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_ROOM_B_USAGE_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "room-c-usage", "Room C usage (kWh)")
                .setNameLocalizations(localizer.getLocaleData(Keys.EB_OPT_ROOM_C_USAGE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.EB_OPT_ROOM_C_USAGE_DESC))
                .setRequired(true)
        ).setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    // 每月水費帳單記錄指令
    Commands.slash("water-bill", "Record monthly water bill")
        .setNameLocalizations(localizer.getLocaleData(Keys.WATER_BILL_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.WATER_BILL_DESC))
        .addOptions(
            OptionData(OptionType.STRING, "bill-month", "Bill month (YYYY-MM format)")
                .setNameLocalizations(localizer.getLocaleData(Keys.WB_OPT_MONTH_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.WB_OPT_MONTH_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "total-usage", "Total water usage (cubic meters)")
                .setNameLocalizations(localizer.getLocaleData(Keys.WB_OPT_TOTAL_USAGE_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.WB_OPT_TOTAL_USAGE_DESC))
                .setRequired(true),
            OptionData(OptionType.NUMBER, "total-amount", "Total bill amount (including fees and discounts)")
                .setNameLocalizations(localizer.getLocaleData(Keys.WB_OPT_TOTAL_AMOUNT_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.WB_OPT_TOTAL_AMOUNT_DESC))
                .setRequired(true)
        ).setDefaultPermissions(DefaultMemberPermissions.ENABLED),

    // 租金概覽指令
    Commands.slash("rent-overview", "Display rent and utility overview for a specific month")
        .setNameLocalizations(localizer.getLocaleData(Keys.RENT_OVERVIEW_NAME))
        .setDescriptionLocalizations(localizer.getLocaleData(Keys.RENT_OVERVIEW_DESC))
        .addOptions(
            OptionData(
                OptionType.STRING,
                "month",
                "Month to display (YYYY-MM format, optional - defaults to current month)"
            )
                .setNameLocalizations(localizer.getLocaleData(Keys.RO_OPT_MONTH_NAME))
                .setDescriptionLocalizations(localizer.getLocaleData(Keys.RO_OPT_MONTH_DESC))
                .setRequired(false)
        ).setDefaultPermissions(DefaultMemberPermissions.ENABLED)
)