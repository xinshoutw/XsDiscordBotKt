package tw.xinshou.plugin.ntustmanager

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.slf4j.LoggerFactory
import tw.xinshou.loader.base.BotLoader.jdaBot
import tw.xinshou.plugin.ntustmanager.announcement.*

/**
 * Main NTUST Manager class responsible for coordinating announcement monitoring
 * and handling new announcements. This class delegates most functionality to
 * specialized components while maintaining the core handleNewAnnouncement function.
 */
object NtustManager {
    private val logger = LoggerFactory.getLogger(NtustManager::class.java)

    // Core components
    private lateinit var cacheManager: AnnouncementCacheManager
    private lateinit var scheduler: AnnouncementScheduler
    private val guild = jdaBot.getGuildById(1407404369753931826L)!!
    private val STUDENT_DORMITORY_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.STUDENT_DORMITORY.id)!! }
    private val STUDENT_AID_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.STUDENT_AID.id)!! }
    private val STUDENT_GUIDANCE_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.STUDENT_GUIDANCE.id)!! }
    private val STUDENT_ACTIVITY_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.STUDENT_ACTIVITY.id)!! }
    private val STUDENT_RESOURCE_ROOM_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.STUDENT_RESOURCE_ROOM.id)!! }
    private val ACADEMIC_MAIN_OFFICE_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.ACADEMIC_MAIN_OFFICE.id)!! }
    private val ACADEMIC_GRADUATE_EDUCATION_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.ACADEMIC_GRADUATE_EDUCATION.id)!! }
    private val ACADEMIC_REGISTRATION_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.ACADEMIC_REGISTRATION.id)!! }
    private val ACADEMIC_COURSE_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.ACADEMIC_COURSE.id)!! }
    private val ACADEMIC_COMPREHENSIVE_BUSINESS_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.ACADEMIC_COMPREHENSIVE_BUSINESS.id)!! }
    private val LANGUAGE_CENTER_RECRUITMENT_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.LANGUAGE_CENTER_RECRUITMENT.id)!! }
    private val LANGUAGE_CENTER_EXEMPTION_REWARD_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.LANGUAGE_CENTER_EXEMPTION_REWARD.id)!! }
    private val LANGUAGE_CENTER_HONOR_LIST_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.LANGUAGE_CENTER_HONOR_LIST.id)!! }
    private val LANGUAGE_CENTER_FRESHMAN_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.LANGUAGE_CENTER_FRESHMAN.id)!! }
    private val LANGUAGE_CENTER_ENGLISH_WORDS_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.LANGUAGE_CENTER_ENGLISH_WORDS.id)!! }
    private val LANGUAGE_CENTER_EXTERNAL_CHANNEL by lazy { guild.getNewsChannelById(AnnouncementChannelId.LANGUAGE_CENTER_EXTERNAL.id)!! }


    init {
        logger.info("NtustManager initialized")
        initializeComponents()
//        clearCache()
        startSystem()
    }

    /**
     * Initializes all the core components
     */
    private fun initializeComponents() {
        // Initialize cache manager
        cacheManager = AnnouncementCacheManager(Event.pluginName)
        cacheManager.initializeCache()

        // Initialize scheduler with cache manager and announcement handler
        scheduler = AnnouncementScheduler(cacheManager, ::handleNewAnnouncement)

        logger.info("All components initialized successfully")
    }

    /**
     * Starts the announcement monitoring system
     */
    private fun startSystem() {
        scheduler.startScheduler()
        logger.info("NTUST announcement monitoring system started")
    }

    /**
     * Splits content into segments that don't exceed the maximum length,
     * breaking at newlines when possible to maintain readability.
     *
     * @param content The content to split
     * @param maxLength The maximum length per segment (default 2000 for Discord)
     * @return List of content segments
     */
    private fun splitContentIntoSegments(content: String, maxLength: Int = 2000): List<String> {
        if (content.length <= maxLength) {
            return listOf(content)
        }

        val segments = mutableListOf<String>()
        var remainingContent = content

        while (remainingContent.length > maxLength) {
            // Find the last newline before the maxLength limit
            val cutPoint = remainingContent.lastIndexOf('\n', maxLength)

            val segmentEnd = if (cutPoint > 0) {
                // Split at the newline
                cutPoint
            } else {
                // No newline found, split at maxLength (fallback)
                maxLength
            }

            // Add the segment
            segments.add(remainingContent.substring(0, segmentEnd))

            // Update remaining content (skip the newline if we split at one)
            remainingContent = if (cutPoint > 0 && segmentEnd < remainingContent.length) {
                remainingContent.substring(segmentEnd + 1) // Skip the newline
            } else {
                remainingContent.substring(segmentEnd)
            }
        }

        // Add the remaining content if any
        if (remainingContent.isNotEmpty()) {
            segments.add(remainingContent)
        }

        return segments
    }

    /**
     * Creates Discord messages from announcement data, splitting content if necessary
     * to ensure no message exceeds 2000 characters.
     *
     * @param announcement The announcement data to create messages from
     * @return List of Discord messages ready to send
     */
    private fun createDiscordMessages(announcement: AnnouncementData): List<net.dv8tion.jda.api.utils.messages.MessageCreateData> {
        val header = "## [${announcement.link.title.trim()}](${announcement.link.url.trim()})"

        if (announcement.content == null) {
            // For null content, just send header + URL
            val messageContent = """
                $header
                
                ${announcement.link.url.trim()}
            """.trimIndent()

            return listOf(MessageCreateBuilder().setContent(messageContent).build())
        }

        // For content announcements, check if we need to split
        val fullContent = """
            $header
            
            ${announcement.content.trim()}
        """.trimIndent()

        if (fullContent.length <= 2000) {
            // Content fits in one message
            return listOf(MessageCreateBuilder().setContent(fullContent).build())
        }

        // Content is too long, need to split
        val messages = mutableListOf<net.dv8tion.jda.api.utils.messages.MessageCreateData>()

        // First message: header + beginning of content
        val availableSpaceForContent = 2000 - header.length - 4 // 4 for "\n\n"
        val contentSegments = splitContentIntoSegments(announcement.content.trim(), availableSpaceForContent)

        // First message with header
        val firstMessageContent = """
            $header
            
            ${contentSegments[0]}
        """.trimIndent()
        messages.add(MessageCreateBuilder().setContent(firstMessageContent).build())

        // Subsequent messages with remaining content segments
        for (i in 1 until contentSegments.size) {
            messages.add(MessageCreateBuilder().setContent(contentSegments[i]).build())
        }

        return messages
    }

    /**
     * Handles a new announcement - this is the core function that remains in NtustManager
     * This is where user-defined announcement processing logic should be implemented
     *
     * @param announcement The new announcement data to process
     */
    private fun handleNewAnnouncement(announcement: AnnouncementData) {
        logger.info("New announcement: ${announcement.title ?: "Unknown Title"} (${announcement.link.type})")

        // Log announcement details
        logger.debug("Announcement details:")
        logger.debug("  Type: ${announcement.link.type}")
        logger.debug("  URL: ${announcement.link.url}")
        logger.debug("  Title: ${announcement.title ?: "Unknown Title"}")
        logger.debug("  Release Date: ${announcement.releaseDate}")
        logger.debug("  Content Length: ${announcement.content?.length ?: 0} characters")
        logger.debug("  Fetched Timestamp: ${announcement.fetchedTimestamp}")

        // TODO: Implement user-defined announcement processing logic
        // This is where you would trigger specific functions to handle the content
        // Examples:
        // - Send notifications to Discord channels
        // - Store in additional databases
        // - Trigger webhooks
        // - Process content for specific keywords
        // - Generate summaries
        // - Forward to other systems

        // Create messages with content splitting if necessary
        val messages = createDiscordMessages(announcement)
        logger.debug("Created ${messages.size} message(s) for announcement")

        // Send all messages to the appropriate channel
        val targetChannel = when (announcement.link.type) {
            AnnouncementType.STUDENT_DORMITORY -> STUDENT_DORMITORY_CHANNEL
            AnnouncementType.STUDENT_AID -> STUDENT_AID_CHANNEL
            AnnouncementType.STUDENT_GUIDANCE -> STUDENT_GUIDANCE_CHANNEL
            AnnouncementType.STUDENT_ACTIVITY -> STUDENT_ACTIVITY_CHANNEL
            AnnouncementType.STUDENT_RESOURCE_ROOM -> STUDENT_RESOURCE_ROOM_CHANNEL
            AnnouncementType.ACADEMIC_MAIN_OFFICE -> ACADEMIC_MAIN_OFFICE_CHANNEL
            AnnouncementType.ACADEMIC_GRADUATE_EDUCATION -> ACADEMIC_GRADUATE_EDUCATION_CHANNEL
            AnnouncementType.ACADEMIC_REGISTRATION -> ACADEMIC_REGISTRATION_CHANNEL
            AnnouncementType.ACADEMIC_COURSE -> ACADEMIC_COURSE_CHANNEL
            AnnouncementType.ACADEMIC_COMPREHENSIVE_BUSINESS -> ACADEMIC_COMPREHENSIVE_BUSINESS_CHANNEL
            AnnouncementType.LANGUAGE_CENTER_RECRUITMENT -> LANGUAGE_CENTER_RECRUITMENT_CHANNEL
            AnnouncementType.LANGUAGE_CENTER_EXEMPTION_REWARD -> LANGUAGE_CENTER_EXEMPTION_REWARD_CHANNEL
            AnnouncementType.LANGUAGE_CENTER_HONOR_LIST -> LANGUAGE_CENTER_HONOR_LIST_CHANNEL
            AnnouncementType.LANGUAGE_CENTER_FRESHMAN -> LANGUAGE_CENTER_FRESHMAN_CHANNEL
            AnnouncementType.LANGUAGE_CENTER_ENGLISH_WORDS -> LANGUAGE_CENTER_ENGLISH_WORDS_CHANNEL
            AnnouncementType.LANGUAGE_CENTER_EXTERNAL -> LANGUAGE_CENTER_EXTERNAL_CHANNEL
        }

        // Send each message sequentially
        messages.forEachIndexed { index, message ->
            targetChannel.sendMessage(message).queue { success ->
                logger.debug("Successfully sent message ${index + 1}/${messages.size} for ${announcement.link.type}")
            }
        }

        logger.info("Announcement processing completed for: ${announcement.title ?: "Unknown Title"}")
    }

    /**
     * Gets all current announcement lists
     * @return Map of announcement types to their current announcement lists
     */
    fun getAllCurrentAnnouncementLists(): Map<AnnouncementType, List<AnnouncementLink>> {
        return if (::cacheManager.isInitialized) {
            cacheManager.getAllCurrentAnnouncementLists()
        } else {
            emptyMap()
        }
    }

    /**
     * Triggers a manual check of all announcements (for testing or immediate updates)
     */
    suspend fun triggerManualCheck() {
        if (::scheduler.isInitialized) {
            scheduler.triggerManualCheck()
        } else {
            logger.warn("Scheduler not initialized, cannot trigger manual check")
        }
    }

    /**
     * Clears all cache data
     */
    fun clearCache() {
        if (::cacheManager.isInitialized) {
            cacheManager.clearCache()
            logger.info("Cache cleared successfully")
        } else {
            logger.warn("Cache manager not initialized, cannot clear cache")
        }
    }

    /**
     * Stops the scheduler and cleans up resources
     */
    fun shutdown() {
        try {
            if (::scheduler.isInitialized) {
                scheduler.shutdown()
            }
            logger.info("NtustManager shutdown completed")
        } catch (e: Exception) {
            logger.error("Error during shutdown", e)
        }
    }
}