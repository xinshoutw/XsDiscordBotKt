package tw.xinshou.plugin.ntustmanager.announcement

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import tw.xinshou.plugin.ntustmanager.service.GeminiApiService
import tw.xinshou.plugin.ntustmanager.util.UrlUtils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class AnnouncementScheduler(
    private val cacheManager: AnnouncementCacheManager,
    private val geminiApiService: GeminiApiService,
    private val onNewAnnouncement: (AnnouncementData) -> Unit
) {
    private val logger = LoggerFactory.getLogger(AnnouncementScheduler::class.java)

    // Configuration constants
    private val cycleTimeSeconds = 3600L // 1 hour = 3600 seconds
    private val maxConcurrentRequests = 5

    // Scheduler for periodic execution
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    // Coroutine scope for concurrent processing
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Mapping of announcement types to their URLs
    private val announcementUrls = mapOf(
        // 學務處
        AnnouncementType.STUDENT_DORMITORY to AnnouncementLink(
            AnnouncementType.STUDENT_DORMITORY, "https://student.ntust.edu.tw/p/403-1053-1436-1.php", "學生宿舍公告"
        ),
        AnnouncementType.STUDENT_AID to AnnouncementLink(
            AnnouncementType.STUDENT_AID, "https://student.ntust.edu.tw/p/403-1053-1437-1.php", "助學措施"
        ),
        AnnouncementType.STUDENT_GUIDANCE to AnnouncementLink(
            AnnouncementType.STUDENT_GUIDANCE, "https://student.ntust.edu.tw/p/403-1053-1439-1.php", "生輔組"
        ),
        AnnouncementType.STUDENT_ACTIVITY to AnnouncementLink(
            AnnouncementType.STUDENT_ACTIVITY, "https://student.ntust.edu.tw/p/403-1053-1440-1.php", "活動組"
        ),
        AnnouncementType.STUDENT_RESOURCE_ROOM to AnnouncementLink(
            AnnouncementType.STUDENT_RESOURCE_ROOM, "https://student.ntust.edu.tw/p/412-1053-8272.php", "資源教室"
        ),

        // 教務處
        AnnouncementType.ACADEMIC_MAIN_OFFICE to AnnouncementLink(
            AnnouncementType.ACADEMIC_MAIN_OFFICE,
            "https://www.academic.ntust.edu.tw/p/403-1048-1413-1.php",
            "處本部公告"
        ),
        AnnouncementType.ACADEMIC_GRADUATE_EDUCATION to AnnouncementLink(
            AnnouncementType.ACADEMIC_GRADUATE_EDUCATION,
            "https://www.academic.ntust.edu.tw/p/403-1048-1414-1.php",
            "研教組公告"
        ),
        AnnouncementType.ACADEMIC_REGISTRATION to AnnouncementLink(
            AnnouncementType.ACADEMIC_REGISTRATION,
            "https://www.academic.ntust.edu.tw/p/403-1048-1415-1.php",
            "註冊組公告"
        ),
        AnnouncementType.ACADEMIC_COURSE to AnnouncementLink(
            AnnouncementType.ACADEMIC_COURSE, "https://www.academic.ntust.edu.tw/p/403-1048-1416-1.php", "課務組公告"
        ),
        AnnouncementType.ACADEMIC_COMPREHENSIVE_BUSINESS to AnnouncementLink(
            AnnouncementType.ACADEMIC_COMPREHENSIVE_BUSINESS,
            "https://www.academic.ntust.edu.tw/p/403-1048-1417-1.php",
            "綜合業務組公告"
        ),

        // 語言中心
        AnnouncementType.LANGUAGE_CENTER_RECRUITMENT to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_RECRUITMENT,
            "https://lc.ntust.edu.tw/p/403-1070-1626-1.php",
            "語言中心徵才公告"
        ),
        AnnouncementType.LANGUAGE_CENTER_EXEMPTION_REWARD to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_EXEMPTION_REWARD,
            "https://lc.ntust.edu.tw/p/403-1070-1627-1.php",
            "語言中心抵免與獎勵公告"
        ),
        AnnouncementType.LANGUAGE_CENTER_HONOR_LIST to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_HONOR_LIST,
            "https://lc.ntust.edu.tw/p/403-1070-1051-1.php",
            "語言中心榮譽榜單"
        ),
        AnnouncementType.LANGUAGE_CENTER_FRESHMAN to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_FRESHMAN,
            "https://lc.ntust.edu.tw/p/403-1070-1628-1.php",
            "語言中心新生相關公告"
        ),
        AnnouncementType.LANGUAGE_CENTER_ENGLISH_WORDS to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_ENGLISH_WORDS,
            "https://lc.ntust.edu.tw/p/403-1070-1828-1.php",
            "語言中心英文單字"
        ),
        AnnouncementType.LANGUAGE_CENTER_EXTERNAL to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_EXTERNAL,
            "https://lc.ntust.edu.tw/p/403-1070-1829-1.php",
            "語言中心校外公告"
        )
    )

    /**
     * Starts the periodic scheduler for checking announcements
     */
    fun startScheduler() {
        scheduler.scheduleAtFixedRate({
            try {
                runBlocking {
                    checkAllAnnouncements()
                }
            } catch (e: Exception) {
                logger.error("Error during scheduled announcement check", e)
            }
        }, 0, cycleTimeSeconds, TimeUnit.SECONDS)

        logger.info("Scheduler started with ${cycleTimeSeconds}s interval")
    }

    /**
     * Checks all announcement sources for updates
     */
    private suspend fun checkAllAnnouncements() {
        logger.info("Starting announcement check cycle")

        val semaphore = Semaphore(maxConcurrentRequests)

        announcementUrls.values.map { link ->
            coroutineScope.async {
                semaphore.withPermit {
                    try {
                        checkAnnouncementUpdates(link)
                    } catch (e: Exception) {
                        logger.error("Error checking announcements for ${link.type}", e)
                    }
                }
            }
        }.awaitAll()
    }

    /**
     * Checks for updates in a specific announcement board by comparing full announcement lists
     */
    private suspend fun checkAnnouncementUpdates(link: AnnouncementLink) {
        val boardData = AnnouncementParser.boardParser(link)
        if (boardData.isEmpty()) {
            logger.warn("No announcements found for ${link.type}")
            return
        }

        // Compare current announcement list with new fetched data
        val changes = cacheManager.compareAnnouncementLists(link.type, boardData)

        // Handle removed announcements
        if (changes.removed.isNotEmpty()) {
            logger.info("Removed announcements detected for ${link.type}: ${changes.removed.size} announcements")
            // Remove announcements from cache
            cacheManager.removeAnnouncementsFromCache(link.type, changes.removed)
            logger.debug("Removed announcements from cache for {}", link.type)
        }

        // Handle new announcements
        if (changes.added.isNotEmpty()) {
            logger.info("New announcements detected for ${link.type}: ${changes.added.size} announcements")
            // Process each new announcement by calling the handler
            processNewAnnouncements(changes.added)
        }

        // Update the current announcement list in cache (regardless of changes)
        cacheManager.updateCurrentAnnouncementList(link.type, boardData)

        if (changes.added.isEmpty() && changes.removed.isEmpty()) {
            logger.debug("No changes detected for {}", link.type)
        } else {
            logger.info("Processed changes for ${link.type}: ${changes.added.size} added, ${changes.removed.size} removed")
        }
    }

    /**
     * Processes new announcements by parsing their content
     */
    private suspend fun processNewAnnouncements(announcements: List<AnnouncementLink>) {
        announcements.forEach { link ->
            try {
                val announcementData = AnnouncementParser.contentParser(link, geminiApiService)
                if (announcementData != null) {
                    // Save to cache
                    cacheManager.saveAnnouncementData(announcementData)

                    // Trigger the callback for new announcement handling
                    onNewAnnouncement(announcementData)
                }
            } catch (e: Exception) {
                logger.error("Error processing announcement ${UrlUtils.extractParagraphId(link.url) ?: link.url}", e)
            }
        }
    }

    /**
     * Stops the scheduler and cleans up resources
     */
    fun shutdown() {
        try {
            scheduler.shutdown()
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
            coroutineScope.cancel()
            logger.info("AnnouncementScheduler shutdown completed")
        } catch (e: Exception) {
            logger.error("Error during scheduler shutdown", e)
        }
    }

    /**
     * Gets scheduler statistics
     * @return Map containing scheduler statistics
     */
    fun getSchedulerStats(): Map<String, Any> {
        return mapOf(
            "cycleTimeSeconds" to cycleTimeSeconds,
            "maxConcurrentRequests" to maxConcurrentRequests,
            "announcementSourcesCount" to announcementUrls.size,
            "isShutdown" to scheduler.isShutdown,
            "isTerminated" to scheduler.isTerminated
        )
    }

    /**
     * Triggers a manual check of all announcements (for testing or immediate updates)
     */
    suspend fun triggerManualCheck() {
        logger.info("Manual announcement check triggered")
        checkAllAnnouncements()
    }
}