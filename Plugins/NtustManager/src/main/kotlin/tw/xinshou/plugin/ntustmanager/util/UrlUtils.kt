package tw.xinshou.plugin.ntustmanager.util

import org.slf4j.LoggerFactory
import tw.xinshou.plugin.ntustmanager.announcement.AnnouncementData
import tw.xinshou.plugin.ntustmanager.announcement.AnnouncementLink
import tw.xinshou.plugin.ntustmanager.announcement.UrlType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object UrlUtils {
    private val logger = LoggerFactory.getLogger(UrlUtils::class.java)

    /**
     * Extracts paragraph ID from href attribute
     * Examples:
     * - "https://student.ntust.edu.tw/p/406-1053-139292,r1436.php?Lang=zh-tw" -> "406-1053-139292,r1436"
     * - "https://www.academic.ntust.edu.tw/p/406-1048-138746,r1413.php?Lang=zh-tw" -> "406-1048-138746,r1413"
     */
    fun extractParagraphId(href: String): String? {
        return try {
            // Look for pattern: /p/PARAGRAPH_ID.php
            val regex = """/p/([^.]+)\.php""".toRegex()
            val matchResult = regex.find(href)
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.debug("Error extracting paragraph ID from: $href", e)
            null
        }
    }

    /**
     * Extracts domain from a complete URL
     * Examples:
     * - "https://student.ntust.edu.tw/p/403-1053-1436-1.php" -> "student.ntust.edu.tw"
     * - "https://www.academic.ntust.edu.tw/p/403-1048-1413-1.php" -> "www.academic.ntust.edu.tw"
     */
    fun extractDomainFromBaseUrl(url: String): String {
        return try {
            val regex = """https://([^/]+)""".toRegex()
            val matchResult = regex.find(url)
            matchResult?.groupValues?.get(1) ?: "ntust.edu.tw"
        } catch (e: Exception) {
            logger.debug("Error extracting domain from: $url", e)
            "ntust.edu.tw"
        }
    }

    /**
     * Validates URL type and determines if content should be processed
     * @param url The URL to validate
     * @return UrlType indicating how the URL should be handled
     */
    fun validateUrlType(url: String): UrlType {
        return when {
            // NTUST announcement pattern: ^https://**.ntust.edu.tw/p/[0-9\-]+\.php$
            url.matches("""^https://.*\.ntust\.edu\.tw/p/[0-9\-,r]+\.php.*$""".toRegex()) -> UrlType.NTUST_ANNOUNCEMENT
            url.startsWith("/") -> UrlType.RELATIVE_PATH
            else -> UrlType.THIRD_PARTY
        }
    }

    /**
     * Constructs complete URL based on href type
     * @param href The href attribute from HTML
     * @param baseUrl The base URL to use for relative paths
     * @return Complete URL
     */
    fun constructCompleteUrl(href: String, baseUrl: String): String {
        return when {
            href.startsWith("https://") -> href // Already complete URL
            href.startsWith("/") -> "https://${extractDomainFromBaseUrl(baseUrl)}$href" // Relative path
            else -> href // Third-party or other format
        }
    }

    /**
     * Creates AnnouncementData for non-NTUST URLs with null content
     * @param link The announcement link
     * @return AnnouncementData with null content
     */
    fun createExternalLinkData(link: AnnouncementLink): AnnouncementData {
        return AnnouncementData(
            link = link,
            title = link.title,
            content = null,
            releaseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }
}