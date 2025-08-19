package tw.xinshou.plugin.ntustmanager.announcement

import org.slf4j.LoggerFactory
import tw.xinshou.loader.mongodb.CacheDbManager
import tw.xinshou.loader.mongodb.ICacheDb
import tw.xinshou.plugin.ntustmanager.util.UrlUtils
import java.util.concurrent.ConcurrentHashMap

class AnnouncementCacheManager(pluginName: String) {
    private val logger = LoggerFactory.getLogger(AnnouncementCacheManager::class.java)

    // Cache database manager for persistent storage
    private val cacheDbManager: CacheDbManager = CacheDbManager(pluginName)

    // Cache collections
    private val currentAnnouncementListsCache: ICacheDb =
        cacheDbManager.getCollection("current_announcement_lists", memoryCache = true)
    private val announcementDataCache: ICacheDb =
        cacheDbManager.getCollection("announcement_data", memoryCache = true)

    // Record keeping - stores the complete list of current announcements for each department (loaded from cache)
    private val currentAnnouncementLists = ConcurrentHashMap<AnnouncementType, List<AnnouncementLink>>()

    /**
     * Converts legacy cached data (LinkedHashTreeMap) to proper AnnouncementLink objects
     * @param legacyList List containing Map objects that need to be converted
     * @return List of properly typed AnnouncementLink objects
     */
    private fun convertLegacyAnnouncementList(legacyList: List<*>): List<AnnouncementLink> {
        return legacyList.mapNotNull { item ->
            try {
                when (item) {
                    is Map<*, *> -> {
                        // Extract fields from the map
                        val typeStr = item["type"]?.toString()
                        val url = item["url"]?.toString()
                        val title = item["title"]?.toString()

                        if (typeStr != null && url != null && title != null) {
                            val announcementType = AnnouncementType.valueOf(typeStr)
                            AnnouncementLink(
                                type = announcementType,
                                url = url,
                                title = title
                            )
                        } else {
                            logger.warn(
                                "Missing required fields in legacy cache item: type={}, url={}, title={}",
                                typeStr,
                                url,
                                title
                            )
                            null
                        }
                    }

                    else -> {
                        logger.warn("Unexpected item type in legacy list: {}", item?.javaClass?.simpleName)
                        null
                    }
                }
            } catch (e: Exception) {
                logger.error("Error converting legacy cache item: {}", item, e)
                null
            }
        }
    }

    /**
     * Initializes the cache by loading data from MongoDB into HashMap
     */
    fun initializeCache() {
        try {
            logger.info("Loading announcement data from cache...")
            logger.debug(
                "Cache collections - Lists: {}, Data: {}",
                currentAnnouncementListsCache.count(), announcementDataCache.count()
            )

            // Load current announcement lists from cache
            val cachedAnnouncementLists = currentAnnouncementListsCache.loadAll()
            logger.debug("Retrieved {} cached announcement list entries from MongoDB", cachedAnnouncementLists.size)

            cachedAnnouncementLists.forEach { (key, value) ->
                try {
                    logger.debug("Processing cache entry: key='{}', value type: {}", key, value::class.simpleName)
                    val announcementType = AnnouncementType.valueOf(key)

                    // Handle both properly typed lists and legacy LinkedHashTreeMap lists
                    val announcementList = when (value) {
                        is List<*> -> {
                            // Check if the list contains properly typed AnnouncementLink objects
                            if (value.isNotEmpty()) {
                                val firstItem = value.first()
                                logger.debug("  First item type: {}", firstItem?.javaClass?.simpleName)

                                when (firstItem) {
                                    is AnnouncementLink -> {
                                        // Already properly typed
                                        @Suppress("UNCHECKED_CAST")
                                        value as List<AnnouncementLink>
                                    }

                                    is Map<*, *> -> {
                                        // Legacy data - convert LinkedHashTreeMap to AnnouncementLink
                                        logger.info("Converting legacy cache data for {}", announcementType)
                                        convertLegacyAnnouncementList(value)
                                    }

                                    else -> {
                                        logger.warn(
                                            "Unknown item type in cached list for {}: {}",
                                            announcementType,
                                            firstItem?.javaClass?.simpleName
                                        )
                                        emptyList()
                                    }
                                }
                            } else {
                                emptyList()
                            }
                        }

                        else -> {
                            logger.warn(
                                "Unexpected cache value type for {}: {}",
                                announcementType,
                                value::class.simpleName
                            )
                            emptyList()
                        }
                    }

                    currentAnnouncementLists[announcementType] = announcementList
                    logger.info(
                        "Loaded cached announcement list for {}: {} announcements",
                        announcementType,
                        announcementList.size
                    )

                    // Log first few announcements for debugging
                    if (announcementList.isNotEmpty()) {
                        val firstAnnouncement = announcementList.first()
                        logger.debug("  First announcement: {} -> {}", firstAnnouncement.title, firstAnnouncement.url)
                    }
                } catch (e: IllegalArgumentException) {
                    logger.warn("Invalid announcement type in cache: {}", key, e)
                } catch (e: ClassCastException) {
                    logger.warn(
                        "Invalid announcement list format in cache for: {} (value type: {})",
                        key,
                        value::class.simpleName,
                        e
                    )
                    // Clear this problematic cache entry
                    logger.info("Clearing problematic cache entry for: {}", key)
                    currentAnnouncementListsCache.remove(key)
                } catch (e: Exception) {
                    logger.error("Unexpected error processing cache entry for: {}", key, e)
                }
            }

            logger.info(
                "Cache initialization completed. Loaded {} announcement lists with total {} announcements",
                currentAnnouncementLists.size, currentAnnouncementLists.values.sumOf { it.size })
        } catch (e: Exception) {
            logger.error("Error initializing cache", e)
        }
    }

    /**
     * Gets the current announcement list for a specific announcement type
     * @param type The announcement type
     * @return The current announcement list, or empty list if not found
     */
    fun getCurrentAnnouncementList(type: AnnouncementType): List<AnnouncementLink> {
        return currentAnnouncementLists[type] ?: emptyList()
    }

    /**
     * Updates the current announcement list for a specific announcement type
     * Updates both HashMap and MongoDB cache
     * @param type The announcement type
     * @param announcements The new announcement list
     */
    fun updateCurrentAnnouncementList(type: AnnouncementType, announcements: List<AnnouncementLink>) {
        currentAnnouncementLists[type] = announcements
        currentAnnouncementListsCache.put(type.name, announcements)
        logger.debug("Updated announcement list for {}: {} announcements", type, announcements.size)
    }

    /**
     * Compares current announcement list with new list and returns changes
     * @param type The announcement type
     * @param newAnnouncements The new announcement list from fetch
     * @return AnnouncementChanges containing added and removed announcements
     */
    fun compareAnnouncementLists(
        type: AnnouncementType,
        newAnnouncements: List<AnnouncementLink>
    ): AnnouncementChanges {
        val currentList = getCurrentAnnouncementList(type)

        // Convert to sets for easier comparison using URL as identifier
        val currentUrls = currentList.map { it.url }.toSet()
        val newUrls = newAnnouncements.map { it.url }.toSet()

        // Find added and removed announcements
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
     * @param announcement The announcement data to save
     */
    fun saveAnnouncementData(announcement: AnnouncementData) {
        val cacheKey =
            "${announcement.link.type.name}_${UrlUtils.extractParagraphId(announcement.link.url) ?: announcement.link.url}"
        announcementDataCache.put(cacheKey, announcement)
        logger.debug("Saved announcement to cache: {}", cacheKey)
    }

    /**
     * Gets announcement data from cache
     * @param type The announcement type
     * @param paragraphId The paragraph ID
     * @return The cached announcement data, or null if not found
     */
    fun getAnnouncementData(type: AnnouncementType, paragraphId: String): AnnouncementData? {
        val cacheKey = "${type.name}_$paragraphId"
        return announcementDataCache.get(cacheKey)
    }

    /**
     * Checks if an announcement exists in cache
     * @param type The announcement type
     * @param paragraphId The paragraph ID
     * @return True if the announcement exists in cache
     */
    fun hasAnnouncementData(type: AnnouncementType, paragraphId: String): Boolean {
        val cacheKey = "${type.name}_$paragraphId"
        return announcementDataCache.containsKey(cacheKey)
    }

    /**
     * Gets all current announcement lists
     * @return Map of announcement types to their current announcement lists
     */
    fun getAllCurrentAnnouncementLists(): Map<AnnouncementType, List<AnnouncementLink>> {
        return currentAnnouncementLists.toMap()
    }

    /**
     * Removes announcements from cache
     * @param type The announcement type
     * @param announcementsToRemove List of announcements to remove from cache
     */
    fun removeAnnouncementsFromCache(type: AnnouncementType, announcementsToRemove: List<AnnouncementLink>) {
        announcementsToRemove.forEach { announcement ->
            val cacheKey = "${type.name}_${UrlUtils.extractParagraphId(announcement.url) ?: announcement.url}"
            announcementDataCache.remove(cacheKey)
            logger.debug("Removed announcement from cache: {}", cacheKey)
        }
    }

    /**
     * Clears all cache data
     */
    fun clearCache() {
        try {
            currentAnnouncementLists.clear()
            currentAnnouncementListsCache.clear()
            announcementDataCache.clear()
            logger.info("Cache cleared successfully")
        } catch (e: Exception) {
            logger.error("Error clearing cache", e)
        }
    }

    /**
     * Gets cache statistics
     * @return Map containing cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        val totalAnnouncements = currentAnnouncementLists.values.sumOf { it.size }
        return mapOf(
            "announcementListsCount" to currentAnnouncementLists.size,
            "totalAnnouncementsTracked" to totalAnnouncements,
            "announcementListsCacheCount" to currentAnnouncementListsCache.count(),
            "announcementDataCacheCount" to announcementDataCache.count()
        )
    }
}