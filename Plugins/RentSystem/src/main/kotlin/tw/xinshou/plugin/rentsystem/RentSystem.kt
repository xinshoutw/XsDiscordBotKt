package tw.xinshou.plugin.rentsystem

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.quartz.CronScheduleBuilder.monthlyOnDayAndHourAndMinute
import org.quartz.JobBuilder.newJob
import org.quartz.Scheduler
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.BotLoader
import tw.xinshou.loader.builtin.messagecreator.MessageCreator
import tw.xinshou.plugin.rentsystem.Event.pluginDirectory
import tw.xinshou.plugin.rentsystem.jobs.RentReminderJob
import tw.xinshou.plugin.rentsystem.jobs.WaterPowerReminderJob
import tw.xinshou.plugin.rentsystem.models.BillItem
import tw.xinshou.plugin.rentsystem.models.FinalBill
import java.util.*

internal object RentSystem {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val creator = MessageCreator(
        pluginDirFile = pluginDirectory,
        defaultLocale = DiscordLocale.CHINESE_TAIWAN
    )
    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()
    private val guild = BotLoader.jdaBot.getGuildById(Event.config.guildId)
        ?: throw IllegalStateException("Guild with ID ${Event.config.guildId} not found.")
    private val forumChannel = guild.getForumChannelById(Event.config.forumChannelId)
        ?: throw IllegalStateException("Forum channel with ID ${Event.config.forumChannelId} not found.")

//    val jsonAdapter: JsonAdapter<JsonDataClass> = JsonFileManager.moshi.adapterReified<JsonDataClass>()
//
//    JsonImpl.jsonFileManager = JsonFileManager(
//    File(PLUGIN_DIR_FILE, "data/data.json"),
//    jsonAdapter,
//    mutableMapOf()
//    )


    fun start() {
        logger.info("Starting RentSystem...")

        // Check if plugin is enabled
        if (!Event.config.enabled) {
            logger.warn("RentSystem is disabled in configuration. Please set 'enabled: true' in config.yaml to use this plugin.")
            return
        }
        
        // initialize forum channel
        val startTime = Calendar.getInstance().apply {
            set(
                Event.config.timeRange.startYear,
                Event.config.timeRange.startMonth - 1,
                1
            ) // Calendar months are 0-based
        }
        val endTime = Calendar.getInstance().apply {
            set(Event.config.timeRange.endYear, Event.config.timeRange.endMonth - 1, 1) // Calendar months are 0-based
        }

        val yearBetween = endTime.get(Calendar.YEAR) - startTime.get(Calendar.YEAR)
        val monthBetween = endTime.get(Calendar.MONTH) - startTime.get(Calendar.MONTH)

        for (i in (yearBetween * 12 + monthBetween) downTo 0) {
            val month = ((startTime.get(Calendar.MONTH) + i) % 12) + 1 // Convert back to 1-based for display
            val year = startTime.get(Calendar.YEAR) + ((startTime.get(Calendar.MONTH) + i) / 12)
            val channelName = Event.config.globalMessages.forumChannelNameFormat
                .replace("%rs@yyyy%", year.toString())
                .replace("%rs@MM%", String.format("%02d", month))

            // check if the channel already exists
            if (forumChannel.threadChannels.any { it.name == channelName }) {
                logger.info("Forum channel '$channelName' already exists, skipping creation.")
                continue
            }

            // create the forum channel
            logger.info("Creating forum channel: $channelName")
            forumChannel.createForumPost(
                channelName,
                MessageCreateBuilder().apply {
                    setContent(Event.config.globalMessages.timeNotYetTitle)
                }.build()
            ).map { it.threadChannel }.queue { thread ->
                logger.info("Created forum channel: ${thread.name} (ID: ${thread.id})")
            }
        }

        // Schedule rent reminder job
        scheduler.scheduleJob(
            newJob(RentReminderJob::class.java)
                .withIdentity("rentReminderJob", "rentSystem")
                .build(),
            newTrigger()
                .withIdentity("rentReminderTrigger", "rentSystem")
                .withSchedule(
                    monthlyOnDayAndHourAndMinute(
                        Event.config.schedule.rentReminderDay,
                        Event.config.schedule.rentReminderHour,
                        Event.config.schedule.rentReminderMinute
                    )
                )
                .build()
        )

        // Schedule utility reminder job
        scheduler.scheduleJob(
            newJob(WaterPowerReminderJob::class.java)
                .withIdentity("waterEnergyReminderJob", "rentSystem")
                .build(),
            newTrigger()
                .withIdentity("waterEnergyReminderTrigger", "rentSystem")
                .withSchedule(
                    monthlyOnDayAndHourAndMinute(
                        Event.config.schedule.utilityReminderDay,
                        Event.config.schedule.utilityReminderHour,
                        Event.config.schedule.utilityReminderMinute
                    )
                )
                .build()
        )

        scheduler.start()
        logger.info("Quartz Scheduler has been started.")
    }

    fun stop() {
        scheduler.shutdown(true)
        logger.info("Quartz Scheduler has been shut down.")
    }

    fun triggerWaterPowerChartGeneration() {
        logger.info("Triggering chart generation process...")
        val finalBill = calculateFinalBill()

        // 2. 根據帳單數據產生 QuickChart URL
        val chartUrl = generateChartUrl(finalBill)
        logger.info("Generated Chart URL: $chartUrl")

//         channel?.sendMessage(chartUrl)?.queue()
        logger.info("Chart URL has been sent to Discord (simulation).")
    }

    /**
     * 根據帳單數據，組裝出 QuickChart 的 URL
     * @param billData 包含所有帳單項目的資料物件
     * @return 一個可以直接使用的 QuickChart 圖片 URL 字串
     */
    private fun generateChartUrl(billData: FinalBill): String {
        val baseUrl = "https://quickchart.io/chart/render/zm-75aec4e1-7a0b-4f87-8d13-b2a3eb093ff8"

        // 處理 data1 (Power) - 提取費用並轉換為逗號分隔的字串
        val powerDataString = billData.powerItems.joinToString(",") { it.cost.toString() }

        // 處理 data2 (Water) - 提取費用並轉換為逗號分隔的字串
        val waterDataString = billData.waterItems.joinToString(",") { it.cost.toString() }

        // 組裝最終的 URL，參數不需要特別編碼，因為內容只是數字和逗號
        return "$baseUrl?data1=$powerDataString&data2=$waterDataString"
    }

    /**
     * 【請替換成你的真實邏輯】
     * 這是計算帳單的模擬函數。
     * 你應該在這裡根據資料庫中的電表/水表數據來計算實際費用。
     * @return 一個包含計算結果的 FinalBill 物件
     */
    private fun calculateFinalBill(): FinalBill {
        // --- 模擬數據 ---
        // 這裡的數據應該來自你的電表/水表計算結果
        val powerItems = listOf(
            BillItem("公用電費", 963.3),
            BillItem("A房電費", 496.5),
            BillItem("B房電費", 313.2),
            BillItem("C房電費", 129.9)
        )
        // 假設水費為 $120，三人均分
        val waterItems = listOf(
            BillItem("A水費", 40.0),
            BillItem("B水費", 40.0),
            BillItem("C水費", 40.0)
        )
        // --- 模擬數據結束 ---

        return FinalBill(powerItems, waterItems)
    }

    fun onButtonInteraction(event: ButtonInteractionEvent) {


    }

    fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!Event.config.enabled) {
            event.reply("❌ RentSystem plugin is disabled.").setEphemeral(true).queue()
            return
        }

        when (event.name) {
            "daily-electricity" -> handleDailyElectricityCommand(event)
            "electricity-bill" -> handleElectricityBillCommand(event)
            "water-bill" -> handleWaterBillCommand(event)
            "rent-overview" -> handleRentOverviewCommand(event)
            else -> {
                event.reply("❌ Unknown command: ${event.name}").setEphemeral(true).queue()
            }
        }
    }

    private fun handleDailyElectricityCommand(event: SlashCommandInteractionEvent) {
        try {
            val publicReading = event.getOption("public")?.asDouble ?: 0.0
            val roomAReading = event.getOption("room-a")?.asDouble ?: 0.0
            val roomBReading = event.getOption("room-b")?.asDouble ?: 0.0
            val roomCReading = event.getOption("room-c")?.asDouble ?: 0.0
            val dateStr = event.getOption("date")?.asString ?: java.time.LocalDate.now().toString()

            // TODO: Validate date format and store readings in database
            // TODO: Update monthly statistics

            event.reply(
                "✅ 每日電表讀數已記錄成功！\n" +
                        "📅 日期: $dateStr\n" +
                        "⚡ 公用電表: ${publicReading} kWh\n" +
                        "🏠 A房電表: ${roomAReading} kWh\n" +
                        "🏠 B房電表: ${roomBReading} kWh\n" +
                        "🏠 C房電表: ${roomCReading} kWh"
            ).setEphemeral(true).queue()

            logger.info("Daily electricity readings recorded by ${event.user.id}: Public=$publicReading, A=$roomAReading, B=$roomBReading, C=$roomCReading, Date=$dateStr")
        } catch (e: Exception) {
            logger.error("Error handling daily electricity command", e)
            event.reply("❌ 記錄電表讀數時發生錯誤: ${e.message}").setEphemeral(true).queue()
        }
    }

    private fun handleElectricityBillCommand(event: SlashCommandInteractionEvent) {
        try {
            val periodStart = event.getOption("period-start")?.asString ?: ""
            val periodEnd = event.getOption("period-end")?.asString ?: ""
            val totalUsage = event.getOption("total-usage")?.asDouble ?: 0.0
            val totalAmount = event.getOption("total-amount")?.asDouble ?: 0.0
            val publicUsage = event.getOption("public-usage")?.asDouble ?: 0.0
            val roomAUsage = event.getOption("room-a-usage")?.asDouble ?: 0.0
            val roomBUsage = event.getOption("room-b-usage")?.asDouble ?: 0.0
            val roomCUsage = event.getOption("room-c-usage")?.asDouble ?: 0.0

            // TODO: Validate date formats and store bill in database
            // TODO: Calculate cost distribution based on usage
            // TODO: Update monthly statistics for affected months

            event.reply(
                "✅ 電費帳單已記錄成功！\n" +
                        "📅 帳單期間: $periodStart ~ $periodEnd\n" +
                        "⚡ 總用電量: ${totalUsage} kWh\n" +
                        "💰 總金額: $${totalAmount}\n" +
                        "📊 用電分配:\n" +
                        "  - 公用: ${publicUsage} kWh\n" +
                        "  - A房: ${roomAUsage} kWh\n" +
                        "  - B房: ${roomBUsage} kWh\n" +
                        "  - C房: ${roomCUsage} kWh"
            ).setEphemeral(true).queue()

            logger.info("Electricity bill recorded by ${event.user.id}: Period=$periodStart-$periodEnd, Total=$totalAmount, Usage=$totalUsage")
        } catch (e: Exception) {
            logger.error("Error handling electricity bill command", e)
            event.reply("❌ 記錄電費帳單時發生錯誤: ${e.message}").setEphemeral(true).queue()
        }
    }

    private fun handleWaterBillCommand(event: SlashCommandInteractionEvent) {
        try {
            val billMonth = event.getOption("bill-month")?.asString ?: ""
            val totalUsage = event.getOption("total-usage")?.asDouble ?: 0.0
            val totalAmount = event.getOption("total-amount")?.asDouble ?: 0.0

            // TODO: Validate month format (YYYY-MM) and store bill in database
            // TODO: Calculate cost distribution (average split)
            // TODO: Update monthly statistics

            event.reply(
                "✅ 水費帳單已記錄成功！\n" +
                        "📅 帳單月份: $billMonth\n" +
                        "💧 總用水量: ${totalUsage} 立方米\n" +
                        "💰 總金額: $${totalAmount}\n" +
                        "👥 平均分攤: $${String.format("%.2f", totalAmount / Event.config.members.size)} 每人"
            ).setEphemeral(true).queue()

            logger.info("Water bill recorded by ${event.user.id}: Month=$billMonth, Usage=$totalUsage, Amount=$totalAmount")
        } catch (e: Exception) {
            logger.error("Error handling water bill command", e)
            event.reply("❌ 記錄水費帳單時發生錯誤: ${e.message}").setEphemeral(true).queue()
        }
    }

    private fun handleRentOverviewCommand(event: SlashCommandInteractionEvent) {
        try {
            val month = event.getOption("month")?.asString ?: java.time.YearMonth.now().toString()

            // TODO: Retrieve monthly statistics from database
            // TODO: Generate overview embed with member data
            // TODO: Include rent payment status, utility costs, etc.

            event.reply(
                "📊 租金與水電費概覽 - $month\n\n" +
                    "🏠 **租金狀況:**\n" +
                    Event.config.members.joinToString("\n") { member ->
                        "  ${member.roomOwnerName} (${member.roomId}房): $${member.monthlyRent} - TODO: 繳納狀況"
                    } + "\n\n" +
                    "⚡ **電費分攤:** TODO: 從資料庫載入\n" +
                    "💧 **水費分攤:** TODO: 從資料庫載入\n" +
                    "🌐 **網路費分攤:** TODO: 從資料庫載入\n" +
                    "📦 **雜費分攤:** TODO: 從資料庫載入\n\n" +
                    "💰 **總計:** TODO: 計算總額"
            ).setEphemeral(true).queue()

            logger.info("Rent overview requested by ${event.user.id} for month: $month")
        } catch (e: Exception) {
            logger.error("Error handling rent overview command", e)
            event.reply("❌ 生成租金概覽時發生錯誤: ${e.message}").setEphemeral(true).queue()
        }
    }
}
