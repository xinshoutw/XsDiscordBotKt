package tw.xinshou.discord.core.builtin.statuschanger

import net.dv8tion.jda.api.entities.Activity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.BotLoader.jdaBot
import tw.xinshou.discord.core.base.SettingsLoader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object StatusChanger {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private var threadPool = Executors.newSingleThreadScheduledExecutor()
    private var botStatusList = SettingsLoader.config.builtinSettings?.statusChangerSetting?.activityMessages

    fun run() {
        botStatusList = SettingsLoader.config.builtinSettings?.statusChangerSetting?.activityMessages
        if (botStatusList.isNullOrEmpty()) {
            logger.info("No bot status messages to display.")
            return
        }
        stop()  // Ensure no previous tasks are running

        threadPool = Executors.newSingleThreadScheduledExecutor()
        threadPool.execute {
            try {
                cycleActivities()
            } catch (e: InterruptedException) {
                logger.info("Status updater thread was interrupted; stopping loop.")
                Thread.currentThread().interrupt()  // Preserve interrupt status
            } catch (e: Exception) {
                logger.error("Error occurred in status updater.", e)
            }
        }
    }

    fun stop() {
        if (!threadPool.isShutdown) {
            threadPool.shutdownNow()  // Attempt to stop all actively executing tasks
            try {
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    logger.warn("Thread pool did not terminate; tasks may still be running.")
                }
            } catch (e: InterruptedException) {
                logger.error("Interrupted during shutdown.", e)
                Thread.currentThread().interrupt()  // Preserve interrupt status
            }
        }
    }

    private fun cycleActivities() {
        while (!Thread.interrupted()) {
            for (status in botStatusList!!) {
                val args = status.split(";")
                if (args.size < 3) {
                    logger.error("Invalid status configuration: {}", status)
                    continue
                }
                try {
                    updateActivity(args)
                } catch (e: IllegalArgumentException) {
                    logger.error("Cannot find activity type: {}", args[0], e)
                }
                if (Thread.interrupted()) {
                    logger.info("Interrupt detected, breaking the cycle.")
                    return
                }
            }
        }
    }

    private fun updateActivity(args: List<String>) {
        val type = Activity.ActivityType.valueOf(args[0])
        val name = args[1]
        val urlOrTime = args[2]

        val activity = if (type == Activity.ActivityType.STREAMING) {
            Activity.streaming(name, urlOrTime)
        } else {
            Activity.of(type, name)
        }

        jdaBot.presence.activity = activity
        try {
            Thread.sleep(urlOrTime.toLong())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()  // Re-interrupt the thread to ensure the status is set
            logger.info("Sleep interrupted, preparing to exit the activity loop.")
        }
    }
}
