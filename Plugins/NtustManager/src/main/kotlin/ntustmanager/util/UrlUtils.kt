package ntustmanager.util

import org.slf4j.LoggerFactory
import ntustmanager.announcement.AnnouncementData
import ntustmanager.announcement.AnnouncementLink
import ntustmanager.announcement.UrlType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object UrlUtils {
    private val logger = LoggerFactory.getLogger(UrlUtils::class.java)

    /**
     * Extracts paragraph ID from href attribute
     */
    fun extractParagraphId(href: String): String? {
        return try {
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
     */
    fun validateUrlType(url: String): UrlType {
        return when {
            url.matches("""^https://.*\.ntust\.edu\.tw/p/[0-9\-,r]+\.php.*$""".toRegex()) -> UrlType.NTUST_ANNOUNCEMENT
            url.startsWith("/") -> UrlType.RELATIVE_PATH
            else -> UrlType.THIRD_PARTY
        }
    }

    /**
     * Constructs complete URL based on href type
     */
    fun constructCompleteUrl(href: String, baseUrl: String): String {
        return when {
            href.startsWith("https://") -> href
            href.startsWith("/") -> "https://${extractDomainFromBaseUrl(baseUrl)}$href"
            else -> href
        }
    }

    /**
     * Creates AnnouncementData for non-NTUST URLs with null content
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
