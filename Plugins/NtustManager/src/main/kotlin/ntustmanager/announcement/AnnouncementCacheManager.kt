package tw.xinshou.discord.plugin.ntustmanager.announcement

import tw.xinshou.discord.core.database.DatabaseProvider
import org.koin.java.KoinJavaComponent.getKoin
import org.slf4j.LoggerFactory
import tw.xinshou.discord.plugin.ntustmanager.util.UrlUtils
import java.util.concurrent.ConcurrentHashMap

class AnnouncementCacheManager(pluginName: String) {
    private val logger = LoggerFactory.getLogger(AnnouncementCacheManager::class.java)

    // TODO: Migrate from old CacheDbManager to v4 DatabaseProvider (MongoDB coroutine client).
    // The old CacheDbManager/ICacheDb/MemoryCacheDb API no longer exists in v4.
    // For now, using in-memory only storage. Persistent cache requires implementing
    // MongoDB collections via DatabaseProvider.

    // Record keeping - stores the complete list of current announcements for each department
    private val currentAnnouncementLists = ConcurrentHashMap<AnnouncementType, List<AnnouncementLink>>()

    /**
     * Initializes the cache (in-memory only for now)
     */
    fun initializeCache() {
        try {
            logger.info("Announcement cache initialized (in-memory mode)")
            logger.info(
                "Cache initialization completed. Loaded {} announcement lists with total {} announcements",
                currentAnnouncementLists.size, currentAnnouncementLists.values.sumOf { it.size })
        } catch (e: Exception) {
            logger.error("Error initializing cache", e)
        }
    }

    /**
     * Gets the current announcement list for a specific announcement type
     */
    fun getCurrentAnnouncementList(type: AnnouncementType): List<AnnouncementLink> {
        return currentAnnouncementLists[type] ?: emptyList()
    }

    /**
     * Updates the current announcement list for a specific announcement type
     */
    fun updateCurrentAnnouncementList(type: AnnouncementType, announcements: List<AnnouncementLink>) {
        currentAnnouncementLists[type] = announcements
        logger.debug("Updated announcement list for {}: {} announcements", type, announcements.size)
    }

    /**
     * Compares current announcement list with new list and returns changes
     */
    fun compareAnnouncementLists(
        type: AnnouncementType,
        newAnnouncements: List<AnnouncementLink>
    ): AnnouncementChanges {
        val currentList = getCurrentAnnouncementList(type)

        val currentUrls = currentList.map { it.url }.toSet()
        val newUrls = newAnnouncements.map { it.url }.toSet()

        val addedUrls = newUrls - currentUrls
        val removedUrls = currentUrls - newUrls

        val addedAnnouncements = newAnnouncements.filter { it.url in addedUrls }
        val removedAnnouncements = currentList.filter { it.url in removedUrls }

        logger.debug(
            "Comparison for {}: {} added, {} removed",
            type,
            addedAnnouncements.size,
            removedAnnouncements.size
        )

        return AnnouncementChanges(addedAnnouncements, removedAnnouncements)
    }

    /**
     * Saves announcement data to cache
     */
    fun saveAnnouncementData(announcement: AnnouncementData) {
        val cacheKey =
            "${announcement.link.type.name}_${UrlUtils.extractParagraphId(announcement.link.url) ?: announcement.link.url}"
        // In-memory only for now
        logger.debug("Saved announcement to cache: {}", cacheKey)
    }

    /**
     * Gets all current announcement lists
     */
    fun getAllCurrentAnnouncementLists(): Map<AnnouncementType, List<AnnouncementLink>> {
        return currentAnnouncementLists.toMap()
    }

    /**
     * Removes announcements from cache
     */
    fun removeAnnouncementsFromCache(type: AnnouncementType, announcementsToRemove: List<AnnouncementLink>) {
        announcementsToRemove.forEach { announcement ->
            val cacheKey = "${type.name}_${UrlUtils.extractParagraphId(announcement.url) ?: announcement.url}"
            logger.debug("Removed announcement from cache: {}", cacheKey)
        }
    }

    /**
     * Clears all cache data
     */
    fun clearCache() {
        try {
            currentAnnouncementLists.clear()
            logger.info("Cache cleared successfully")
        } catch (e: Exception) {
            logger.error("Error clearing cache", e)
        }
    }

    /**
     * Gets cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        val totalAnnouncements = currentAnnouncementLists.values.sumOf { it.size }
        return mapOf(
            "announcementListsCount" to currentAnnouncementLists.size,
            "totalAnnouncementsTracked" to totalAnnouncements,
        )
    }
}
