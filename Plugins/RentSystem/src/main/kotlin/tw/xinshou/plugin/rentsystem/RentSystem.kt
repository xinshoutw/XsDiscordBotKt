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
import tw.xinshou.plugin.rentsystem.Event.PLUGIN_DIR_FILE
import tw.xinshou.plugin.rentsystem.jobs.RentReminderJob
import tw.xinshou.plugin.rentsystem.jobs.WaterPowerReminderJob
import tw.xinshou.plugin.rentsystem.models.BillItem
import tw.xinshou.plugin.rentsystem.models.FinalBill
import java.io.File
import java.util.*

internal object RentSystem {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val creator = MessageCreator(
        langDirFile = File(PLUGIN_DIR_FILE, "lang"),
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
        // initialize forum channel
        val startTime = Calendar.getInstance().apply {
            set(Event.config.time.startTime.year, Event.config.time.startTime.month, 1)
        }
        val endTime = Calendar.getInstance().apply {
            set(Event.config.time.endTime.year, Event.config.time.endTime.month, 1)
        }

        val yearBetween = endTime.get(Calendar.YEAR) - startTime.get(Calendar.YEAR)
        val monthBetween = endTime.get(Calendar.MONTH) - startTime.get(Calendar.MONTH)

        for (i in (yearBetween * 12 + monthBetween) downTo 0) {
            val month = ((startTime.get(Calendar.MONTH) + i) % 12)
            val year = startTime.get(Calendar.YEAR) + ((startTime.get(Calendar.MONTH) + i) / 12)
            val channelName = Event.config.forumChannelNameFormat
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
                    setContent("時間未到")
                }.build()
            ).map { it.threadChannel }.queue { thread ->
                logger.info("Created forum channel: ${thread.name} (ID: ${thread.id})")
            }
        }

        scheduler.scheduleJob(
            newJob(RentReminderJob::class.java)
                .withIdentity("rentReminderJob", "rentSystem")
                .build(),
            newTrigger()
                .withIdentity("rentReminderTrigger", "rentSystem")
                .withSchedule(monthlyOnDayAndHourAndMinute(25, 9, 0)) // 25號早上9點
                .build()
        )

        scheduler.scheduleJob(
            newJob(WaterPowerReminderJob::class.java)
                .withIdentity("waterEnergyReminderJob", "rentSystem")
                .build(),
            newTrigger()
                .withIdentity("waterEnergyReminderTrigger", "rentSystem")
                .withSchedule(monthlyOnDayAndHourAndMinute(20, 20, 0)) // 每月20號晚上8點
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


    }
}
