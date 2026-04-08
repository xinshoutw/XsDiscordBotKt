package tw.xinshou.discord.core.i18n

import net.dv8tion.jda.api.interactions.DiscordLocale

class LocaleMap(
    private val data: Map<DiscordLocale, String>,
    private val defaultLocale: DiscordLocale,
) {
    fun resolve(locale: DiscordLocale): String =
        data[locale] ?: data[defaultLocale] ?: data.values.firstOrNull() ?: ""

    fun resolve(locale: DiscordLocale, fallback: DiscordLocale): String =
        data[locale] ?: data[fallback] ?: data[defaultLocale] ?: ""

    fun toMap(): Map<DiscordLocale, String> = data
}
