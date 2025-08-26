package tw.xinshou.plugin.ntustmanager.announcement

data class AnnouncementLink(
    val type: AnnouncementType, // the type of the announcement
    val url: String, // complete URL of the announcement
    val title: String, // the title of the announcement
)

data class AnnouncementData(
    val link: AnnouncementLink,
    val title: String,
    val content: String?, // the content of the announcement can be null if it's a third-party link
    val releaseDate: String, // the date in format "yyyy-mm-dd"
    val fetchedTimestamp: Long = System.currentTimeMillis(), // timestamp in milliseconds since epoch
)

data class AnnouncementChanges(
    val added: List<AnnouncementLink>, // newly added announcements
    val removed: List<AnnouncementLink> // removed announcements
)