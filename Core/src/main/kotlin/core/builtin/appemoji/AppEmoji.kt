package tw.xinshou.discord.core.builtin.appemoji

import net.dv8tion.jda.api.entities.emoji.Emoji
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.discord.core.base.BotLoader.jdaBot


object AppEmoji {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        jdaBot.retrieveApplicationEmojis().queue() // load into cache
    }

    /**
     * 獲取應用表情符號字串
     *
     * @param emojiId 表情符號 ID
     * @return 對應的表情符號，或 null 如果未找到
     */
    fun get(emojiId: Long): String? {
        return jdaBot.getEmojiById(emojiId)?.asMention ?: run {
            logger.warn("Emoji with ID $emojiId not found.")
            null
        }
    }

    /**
     * 獲取應用表情符號字串
     *
     * @param emojiId 表情符號 ID
     * @return 對應的表情符號，或 null 如果未找到
     */
    fun get(emojiId: String): String? {
        return jdaBot.getEmojiById(emojiId)?.asMention ?: run {
            logger.warn("Emoji with ID $emojiId not found.")
            null
        }
    }

    /**
     * 獲取應用表情符號
     *
     * @param emojiId 表情符號 ID
     * @return 對應的表情符號，或 null 如果未找到
     */
    fun getEmoji(emojiId: Long): Emoji? {
        return jdaBot.getEmojiById(emojiId) ?: run {
            logger.warn("Emoji with ID $emojiId not found.")
            null
        }
    }

    /**
     * 獲取應用表情符號
     *
     * @param emojiId 表情符號 ID
     * @return 對應的表情符號，或 null 如果未找到
     */
    fun getEmoji(emojiId: String): Emoji? {
        return jdaBot.getEmojiById(emojiId) ?: run {
            logger.warn("Emoji with ID $emojiId not found.")
            null
        }
    }
}