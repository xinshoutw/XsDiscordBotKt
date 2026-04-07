package ntustmanager.announcement

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import ntustmanager.Event
import ntustmanager.service.GeminiApiService
import ntustmanager.util.UrlUtils
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class AnnouncementScheduler(
    private val cacheManager: AnnouncementCacheManager,
    private val geminiApiService: GeminiApiService,
    private val onNewAnnouncement: (AnnouncementData) -> Unit
) {
    private val logger = LoggerFactory.getLogger(AnnouncementScheduler::class.java)

    private val cycleTimeSeconds = Event.pluginConfig.fetchInterval
    private val maxConcurrentRequests = 5

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val announcementUrls = mapOf(
        AnnouncementType.STUDENT_DORMITORY to AnnouncementLink(
            AnnouncementType.STUDENT_DORMITORY, "https://student.ntust.edu.tw/p/403-1053-1436-1.php", "\u5B78\u751F\u5BBF\u820D\u516C\u544A"
        ),
        AnnouncementType.STUDENT_AID to AnnouncementLink(
            AnnouncementType.STUDENT_AID, "https://student.ntust.edu.tw/p/403-1053-1437-1.php", "\u52A9\u5B78\u63AA\u65BD"
        ),
        AnnouncementType.STUDENT_GUIDANCE to AnnouncementLink(
            AnnouncementType.STUDENT_GUIDANCE, "https://student.ntust.edu.tw/p/403-1053-1439-1.php", "\u751F\u8F14\u7D44"
        ),
        AnnouncementType.STUDENT_ACTIVITY to AnnouncementLink(
            AnnouncementType.STUDENT_ACTIVITY, "https://student.ntust.edu.tw/p/403-1053-1440-1.php", "\u6D3B\u52D5\u7D44"
        ),
        AnnouncementType.STUDENT_RESOURCE_ROOM to AnnouncementLink(
            AnnouncementType.STUDENT_RESOURCE_ROOM, "https://student.ntust.edu.tw/p/412-1053-8272.php", "\u8CC7\u6E90\u6559\u5BA4"
        ),
        AnnouncementType.STUDENT_MAIN_OFFICE to AnnouncementLink(
            AnnouncementType.STUDENT_MAIN_OFFICE, "https://student.ntust.edu.tw/p/403-1053-1435-1.php", "\u5B78\u52D9\u8655\u672C\u90E8\u516C\u544A"
        ),
        AnnouncementType.ACADEMIC_MAIN_OFFICE to AnnouncementLink(
            AnnouncementType.ACADEMIC_MAIN_OFFICE,
            "https://www.academic.ntust.edu.tw/p/403-1048-1413-1.php",
            "\u8655\u672C\u90E8\u516C\u544A"
        ),
        AnnouncementType.ACADEMIC_GRADUATE_EDUCATION to AnnouncementLink(
            AnnouncementType.ACADEMIC_GRADUATE_EDUCATION,
            "https://www.academic.ntust.edu.tw/p/403-1048-1414-1.php",
            "\u7814\u6559\u7D44\u516C\u544A"
        ),
        AnnouncementType.ACADEMIC_REGISTRATION to AnnouncementLink(
            AnnouncementType.ACADEMIC_REGISTRATION,
            "https://www.academic.ntust.edu.tw/p/403-1048-1415-1.php",
            "\u8A3B\u518A\u7D44\u516C\u544A"
        ),
        AnnouncementType.ACADEMIC_COURSE to AnnouncementLink(
            AnnouncementType.ACADEMIC_COURSE, "https://www.academic.ntust.edu.tw/p/403-1048-1416-1.php",
            "\u8AB2\u52D9\u7D44\u516C\u544A"
        ),
        AnnouncementType.ACADEMIC_COMPREHENSIVE_BUSINESS to AnnouncementLink(
            AnnouncementType.ACADEMIC_COMPREHENSIVE_BUSINESS,
            "https://www.academic.ntust.edu.tw/p/403-1048-1417-1.php",
            "\u7D9C\u5408\u696D\u52D9\u7D44\u516C\u544A"
        ),
        AnnouncementType.LANGUAGE_CENTER_RECRUITMENT to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_RECRUITMENT,
            "https://lc.ntust.edu.tw/p/403-1070-1626-1.php",
            "\u8A9E\u8A00\u4E2D\u5FC3\u5FB5\u624D\u516C\u544A"
        ),
        AnnouncementType.LANGUAGE_CENTER_EXEMPTION_REWARD to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_EXEMPTION_REWARD,
            "https://lc.ntust.edu.tw/p/403-1070-1627-1.php",
            "\u8A9E\u8A00\u4E2D\u5FC3\u62B5\u514D\u8207\u734E\u52F5\u516C\u544A"
        ),
        AnnouncementType.LANGUAGE_CENTER_HONOR_LIST to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_HONOR_LIST,
            "https://lc.ntust.edu.tw/p/403-1070-1051-1.php",
            "\u8A9E\u8A00\u4E2D\u5FC3\u69AE\u8B7D\u699C\u55AE"
        ),
        AnnouncementType.LANGUAGE_CENTER_FRESHMAN to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_FRESHMAN,
            "https://lc.ntust.edu.tw/p/403-1070-1628-1.php",
            "\u8A9E\u8A00\u4E2D\u5FC3\u65B0\u751F\u76F8\u95DC\u516C\u544A"
        ),
        AnnouncementType.LANGUAGE_CENTER_ENGLISH_WORDS to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_ENGLISH_WORDS,
            "https://lc.ntust.edu.tw/p/403-1070-1828-1.php",
            "\u8A9E\u8A00\u4E2D\u5FC3\u82F1\u6587\u55AE\u5B57"
        ),
        AnnouncementType.LANGUAGE_CENTER_EXTERNAL to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_EXTERNAL,
            "https://lc.ntust.edu.tw/p/403-1070-1829-1.php",
            "\u8A9E\u8A00\u4E2D\u5FC3\u6821\u5916\u516C\u544A"
        ),
        AnnouncementType.LANGUAGE_CENTER_ACTIVITY to AnnouncementLink(
            AnnouncementType.LANGUAGE_CENTER_ACTIVITY,
            "https://lc.ntust.edu.tw/p/403-1070-1050-1.php",
            "\u8A9E\u8A00\u4E2D\u5FC3\u6D3B\u52D5\u516C\u544A"
        )
    )

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

    private suspend fun checkAllAnnouncements() {
        logger.debug("Starting announcement check cycle")

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

    private suspend fun checkAnnouncementUpdates(link: AnnouncementLink) {
        val boardData = AnnouncementParser.boardParser(link)
        if (boardData.isEmpty()) {
            logger.warn("No announcements found for ${link.type}")
            return
        }

        val validBoardData = filterValidAnnouncements(boardData)

        if (validBoardData.size < boardData.size) {
            val emptyCount = boardData.size - validBoardData.size
            logger.warn(
                "Found $emptyCount announcements with empty/missing content for ${link.type}. " +
                        "These announcements exist but have no actual published content. " +
                        "This may require plugin updates to fix HTML extraction methods or Regex patterns."
            )
        }

        if (validBoardData.isEmpty()) {
            logger.warn("No valid announcements with content found for ${link.type}")
            return
        }

        val changes = cacheManager.compareAnnouncementLists(link.type, validBoardData)

        if (changes.removed.isNotEmpty()) {
            logger.info("Removed announcements detected for ${link.type}: ${changes.removed.size} announcements")
            cacheManager.removeAnnouncementsFromCache(link.type, changes.removed)
            logger.debug("Removed announcements from cache for {}", link.type)
        }

        if (changes.added.isNotEmpty()) {
            logger.info("New announcements detected for ${link.type}: ${changes.added.size} announcements")
            processNewAnnouncements(changes.added)
        }

        cacheManager.updateCurrentAnnouncementList(link.type, validBoardData)

        if (changes.added.isEmpty() && changes.removed.isEmpty()) {
            logger.debug("No changes detected for {}", link.type)
        } else {
            logger.info("Processed changes for ${link.type}: ${changes.added.size} added, ${changes.removed.size} removed")
        }
    }

    private fun filterValidAnnouncements(announcements: List<AnnouncementLink>): List<AnnouncementLink> {
        return announcements.filter { announcement ->
            val hasValidTitle = announcement.title.isNotBlank()
            val hasValidUrl = announcement.url.isNotBlank()

            if (!hasValidTitle || !hasValidUrl) {
                logger.debug(
                    "Announcement has invalid structure: title='${announcement.title}', url='${announcement.url}'"
                )
            }

            hasValidTitle && hasValidUrl
        }
    }

    private suspend fun processNewAnnouncements(announcements: List<AnnouncementLink>) {
        announcements.forEach { link ->
            try {
                val announcementData = AnnouncementParser.contentParser(link, geminiApiService)

                if (announcementData == null) {
                    logger.warn("Failed to parse announcement content for ${link.title} (${link.url})")
                    return@forEach
                }

                cacheManager.saveAnnouncementData(announcementData)

                onNewAnnouncement(announcementData)

                logger.debug("Successfully processed new announcement: ${link.title}")

            } catch (e: Exception) {
                logger.error("Error processing announcement ${UrlUtils.extractParagraphId(link.url) ?: link.url}", e)
            }
        }
    }

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

    fun getSchedulerStats(): Map<String, Any> {
        return mapOf(
            "cycleTimeSeconds" to cycleTimeSeconds,
            "maxConcurrentRequests" to maxConcurrentRequests,
            "announcementSourcesCount" to announcementUrls.size,
            "isShutdown" to scheduler.isShutdown,
            "isTerminated" to scheduler.isTerminated
        )
    }

    suspend fun triggerManualCheck() {
        logger.info("Manual announcement check triggered")
        checkAllAnnouncements()
    }
}
