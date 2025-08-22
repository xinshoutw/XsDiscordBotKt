package tw.xinshou.core.localizations

import net.dv8tion.jda.api.interactions.DiscordLocale

/**
 * Extends HashMap to support locale-specific strings with a default locale fallback mechanism.
 * This class is designed to handle localized text data for different regions and provide a default
 * text if a specific locale's text is not available.
 */
class LocalStringMap : HashMap<DiscordLocale, String>() {
    private lateinit var defaultLocale: DiscordLocale  // Holds the default locale used as a fallback.

    /**
     * Sets the default locale used for fallback when a locale-specific text is not available.
     *
     * @param defaultLocale The default DiscordLocale to be used when the requested locale's text is not found.
     */
    internal fun setDefaultLocale(defaultLocale: DiscordLocale) {
        this.defaultLocale = defaultLocale
    }

    /**
     * Retrieves the text for the specified locale. If the text for the requested locale is not present,
     * it falls back to the default locale's text.
     *
     * @param key The DiscordLocale for which the text is requested.
     * @return The localized text, or the default locale's text if the specific text is not available.
     */
    override fun get(key: DiscordLocale): String {
        return super.get(key) ?: super.get(defaultLocale)!!
    }
}
