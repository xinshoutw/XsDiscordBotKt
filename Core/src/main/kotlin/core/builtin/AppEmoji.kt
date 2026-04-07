package core.builtin

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji

class AppEmoji(private val jda: JDA) {
    private val cache = mutableMapOf<Long, ApplicationEmoji>()

    fun initialize() {
        jda.retrieveApplicationEmojis().queue { emojis ->
            cache.clear()
            emojis.forEach { cache[it.idLong] = it }
        }
    }

    fun get(id: Long): ApplicationEmoji? = cache[id]
    fun get(id: String): ApplicationEmoji? = cache[id.toLongOrNull()]
}
