package tw.xinshou.discord.core.i18n

import net.dv8tion.jda.api.interactions.DiscordLocale

fun parseDiscordLocale(tag: String): DiscordLocale? {
    return try {
        DiscordLocale.from(tag)
    } catch (_: Exception) {
        try {
            DiscordLocale.from(tag.replace('_', '-'))
        } catch (_: Exception) {
            null
        }
    }
}
