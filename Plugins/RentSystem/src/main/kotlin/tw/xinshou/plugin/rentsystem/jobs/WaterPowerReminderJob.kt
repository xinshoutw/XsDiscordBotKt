package tw.xinshou.plugin.rentsystem.jobs

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.plugin.rentsystem.RentSystem

/**
 * 這是水電費圖表產生的任務。
 * 它會在排程器指定的時間執行。
 */
internal class WaterPowerReminderJob : Job {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun execute(context: JobExecutionContext) {
        logger.info("【Scheduled Job】Executing Water & Power chart generation...")

        // 透過 Event 單例物件來取得 RentSystem 的實例並呼叫對應的函數
        // 這是為了讓 Job 和主要邏輯解耦
        RentSystem.triggerWaterPowerChartGeneration()
    }
}
