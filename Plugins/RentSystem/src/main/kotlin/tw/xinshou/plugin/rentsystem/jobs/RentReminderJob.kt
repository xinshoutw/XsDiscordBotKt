package tw.xinshou.plugin.rentsystem.jobs

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class RentReminderJob : Job {
    // will be executed at the 25th day of each month.
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun execute(context: JobExecutionContext) {
        logger.info("【排程任務】執行房租提醒！")
        // val jda = context.jobDetail.jobDataMap["jda_instance"] as JDA
    }
}