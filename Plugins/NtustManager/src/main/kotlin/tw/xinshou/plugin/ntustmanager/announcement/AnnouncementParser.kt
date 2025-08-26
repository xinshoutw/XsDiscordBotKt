package tw.xinshou.plugin.ntustmanager.announcement

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import tw.xinshou.plugin.ntustmanager.service.GeminiApiService
import tw.xinshou.plugin.ntustmanager.util.HtmlMinifier
import tw.xinshou.plugin.ntustmanager.util.UrlUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AnnouncementParser {
    private val logger = LoggerFactory.getLogger(AnnouncementParser::class.java)
    private const val REQUEST_TIMEOUT_SECONDS = 30L

    // HTTP client for making requests
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
        .build()

    /**
     * Parses an announcement board page to extract basic announcement information
     * @param link The announcement link containing complete URL
     * @return List of AnnouncementLink objects with complete URLs
     */
    suspend fun boardParser(link: AnnouncementLink): List<AnnouncementLink> {
        val url = link.url

        return withContext(Dispatchers.IO) {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() != 200) {
                    logger.warn("HTTP ${response.statusCode()} for $url")
                    return@withContext emptyList()
                }

                val document = Ksoup.parse(response.body())
                parseAnnouncementList(document, link)

            } catch (e: Exception) {
                logger.error("Error parsing board for ${link.type}: $url", e)
                emptyList()
            }
        }
    }

    /**
     * Parses the content of a specific announcement
     * @param link The announcement link with complete URL
     * @param geminiApiService The Gemini API service for processing HTML content
     * @return AnnouncementData with full content, or null if parsing fails
     */
    suspend fun contentParser(link: AnnouncementLink, geminiApiService: GeminiApiService): AnnouncementData? {
        val url = link.url

        // Validate URL type and determine if content should be processed
        val urlType = UrlUtils.validateUrlType(url)

        // For non-NTUST announcement URLs, return AnnouncementData with null content
        if (urlType != UrlType.NTUST_ANNOUNCEMENT) {
            logger.info("Non-NTUST announcement URL detected: $url (type: $urlType), setting content to null")
            return UrlUtils.createExternalLinkData(link)
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() != 200) {
                    logger.warn("HTTP ${response.statusCode()} for $url")
                    return@withContext null
                }

                val document = Ksoup.parse(response.body())
                parseAnnouncementContent(document, link, geminiApiService)

            } catch (e: Exception) {
                logger.error("Error parsing content for ${UrlUtils.extractParagraphId(link.url) ?: link.url}: $url", e)
                null
            }
        }
    }

    /**
     * Parses the HTML document to extract announcement list
     * Handles different formats for different departments
     */
    private fun parseAnnouncementList(document: Document, baseLink: AnnouncementLink): List<AnnouncementLink> {
        logger.debug("Parsing announcement list for {}", baseLink.type)

        return try {
            val announcements = mutableListOf<AnnouncementLink>()

            when {
                isLanguageCenter(baseLink.type) -> {
                    // Language Center uses div structure instead of table
                    val announcementRows = document.select("div.row.listBS")

                    for (row in announcementRows) {
                        val titleDiv =
                            row.select("div.d-item.d-title.col-sm-12 div.mbox div.d-txt div.mtitle a").firstOrNull()
                        if (titleDiv == null) {
                            logger.debug("No title link found in Language Center row for {}", baseLink.type)
                            continue
                        }

                        val title = titleDiv.text().trim()
                        val href = titleDiv.attr("href")

                        // Construct complete URL based on href type
                        val completeUrl = UrlUtils.constructCompleteUrl(href, baseLink.url)

                        // Create announcement link
                        val announcementLink = AnnouncementLink(
                            type = baseLink.type,
                            url = completeUrl,
                            title = title
                        )

                        announcements.add(announcementLink)
                        logger.debug("Found Language Center announcement: $title -> $completeUrl")
                    }
                }

                else -> {
                    // Student Affairs and Academic Affairs use table structure
                    val table = document.select("table.listTB.table").firstOrNull()
                    if (table == null) {
                        logger.warn("No announcement table found for ${baseLink.type}")
                        return emptyList()
                    }

                    // Get all table rows in tbody (skip header)
                    val rows = table.select("tbody tr")

                    for (row in rows) {
                        val cells = row.select("td")
                        if (cells.size < 2) continue

                        val (titleCell, _) = when {
                            // Student Affairs: title in first column, date in second
                            isStudentAffairs(baseLink.type) -> Pair(cells[0], cells[1])
                            // Academic Affairs: date in first column, title in second
                            isAcademicAffairs(baseLink.type) -> Pair(cells[1], cells[0])
                            else -> {
                                logger.warn("Unknown department type: ${baseLink.type}")
                                continue
                            }
                        }

                        // Extract title and link
                        val titleLink = titleCell.select("a").firstOrNull()
                        if (titleLink == null) {
                            logger.debug("No link found in title cell for {}", baseLink.type)
                            continue
                        }

                        val title = titleLink.text().trim()
                        val href = titleLink.attr("href")

                        // Filter for STUDENT_MAIN_OFFICE: only include announcements with "【處本部】" prefix
                        if (baseLink.type == AnnouncementType.STUDENT_MAIN_OFFICE && !title.startsWith("【處本部】")) {
                            logger.debug("Skipping announcement for STUDENT_MAIN_OFFICE without '【處本部】' prefix: $title")
                            continue
                        }

                        // Construct complete URL based on href type
                        val completeUrl = UrlUtils.constructCompleteUrl(href, baseLink.url)

                        // Create an announcement link
                        val announcementLink = AnnouncementLink(
                            type = baseLink.type,
                            url = completeUrl,
                            title = title
                        )

                        announcements.add(announcementLink)
                        logger.debug("Found announcement: $title -> $completeUrl")
                    }
                }
            }

            logger.debug("Parsed {} announcements for {}", announcements.size, baseLink.type)
            announcements

        } catch (e: Exception) {
            logger.error("Error parsing announcement list for ${baseLink.type}", e)
            emptyList()
        }
    }

    /**
     * Parses the HTML document to extract announcement content using Gemini API
     * Extracts entire HTML content from the main container and processes it with AI
     */
    private suspend fun parseAnnouncementContent(
        document: Document,
        link: AnnouncementLink,
        geminiApiService: GeminiApiService
    ): AnnouncementData? {
        val urlId = UrlUtils.extractParagraphId(link.url) ?: link.url
        logger.debug("Parsing announcement content for $urlId")

        return try {
            // First, find the main content container with id="Dyn_2_3" and class="M23"
            // Second, find the main content container with class="M23"
            val mainContainer =
                document.select("div#Dyn_2_3.M23").firstOrNull()
                    ?: document.select("div.M23").firstOrNull()
            if (mainContainer == null) {
                logger.warn("No main content container found for announcement $urlId")
                return null
            }

            // Extract the entire HTML content from the main container
            val rawHtml = mainContainer.outerHtml()
            logger.debug("Extracted raw HTML content for $urlId (length: {} chars)", rawHtml.length)

            // Minify the HTML content
            val minifiedHtml = HtmlMinifier.minify(rawHtml)
            logger.debug(
                "Minified HTML content for $urlId (length: {} chars, {}% reduction)",
                minifiedHtml.length,
                if (rawHtml.isNotEmpty()) ((rawHtml.length - minifiedHtml.length) * 100 / rawHtml.length) else 0
            )

            logger.info("Sending HTML content to Gemini API for processing for $urlId")
            // Process the minified HTML with Gemini API
            val processedContent = geminiApiService.processHtmlContentWithRetry(minifiedHtml, link)

            if (processedContent == null) {
                logger.error("Failed to process HTML content with Gemini API for $urlId")
                return null
            }

            logger.info(
                "Successfully processed HTML content with Gemini API for $urlId (processed length: {} chars)",
                processedContent.length
            )

            // Determine release date
            val releaseDate = when {
                isLanguageCenter(link.type) -> {
                    // Language Center has no publication date, use current date
                    val currentDate = LocalDate.now()
                    currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                else -> {
                    // For other departments, use current date as fallback
                    val currentDate = LocalDate.now()
                    currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }
            }

            AnnouncementData(
                link = link,
                title = link.title, // Use the title from the link (extracted during board parsing)
                content = processedContent, // Use the Gemini-processed content
                releaseDate = releaseDate
            )

        } catch (e: Exception) {
            logger.error("Error parsing announcement content for $urlId", e)
            null
        }
    }

    /**
     * Checks if the announcement type belongs to Student Affairs
     */
    private fun isStudentAffairs(type: AnnouncementType): Boolean {
        return when (type) {
            AnnouncementType.STUDENT_DORMITORY,
            AnnouncementType.STUDENT_AID,
            AnnouncementType.STUDENT_GUIDANCE,
            AnnouncementType.STUDENT_ACTIVITY,
            AnnouncementType.STUDENT_RESOURCE_ROOM,
            AnnouncementType.STUDENT_MAIN_OFFICE -> true

            else -> false
        }
    }

    /**
     * Checks if the announcement type belongs to Academic Affairs
     */
    private fun isAcademicAffairs(type: AnnouncementType): Boolean {
        return when (type) {
            AnnouncementType.ACADEMIC_MAIN_OFFICE,
            AnnouncementType.ACADEMIC_GRADUATE_EDUCATION,
            AnnouncementType.ACADEMIC_REGISTRATION,
            AnnouncementType.ACADEMIC_COURSE,
            AnnouncementType.ACADEMIC_COMPREHENSIVE_BUSINESS -> true

            else -> false
        }
    }

    /**
     * Checks if the announcement type belongs to Language Center
     */
    private fun isLanguageCenter(type: AnnouncementType): Boolean {
        return when (type) {
            AnnouncementType.LANGUAGE_CENTER_RECRUITMENT,
            AnnouncementType.LANGUAGE_CENTER_EXEMPTION_REWARD,
            AnnouncementType.LANGUAGE_CENTER_HONOR_LIST,
            AnnouncementType.LANGUAGE_CENTER_FRESHMAN,
            AnnouncementType.LANGUAGE_CENTER_ENGLISH_WORDS,
            AnnouncementType.LANGUAGE_CENTER_EXTERNAL -> true

            else -> false
        }
    }
}