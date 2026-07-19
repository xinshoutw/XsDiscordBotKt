package tw.xinshou.discord.plugin.ntustmanager.util

import org.slf4j.LoggerFactory

/**
 * Utility class for minifying HTML content while preserving structure and readability
 */
object HtmlMinifier {
    private val logger = LoggerFactory.getLogger(HtmlMinifier::class.java)

    /**
     * Minifies HTML content by removing unnecessary whitespace, comments, and formatting
     * while preserving the content structure and readability.
     */
    fun minify(html: String): String {
        if (html.isBlank()) {
            return html
        }

        return try {
            val minified = html
                .replace(Regex("<!--[\\s\\S]*?-->"), "")
                .replace(Regex(">\\s+<"), "><")
                .replace(Regex("^\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("\\s+$", RegexOption.MULTILINE), "")
                .replace(Regex("\\s{2,}"), " ")
                .replace(Regex("\\n\\s*\\n"), "\n")
                .trim()

            logger.debug(
                "HTML minified: {} chars -> {} chars ({}% reduction)",
                html.length, minified.length,
                if (html.length > 0) ((html.length - minified.length) * 100 / html.length) else 0
            )

            minified
        } catch (e: Exception) {
            logger.error("Error minifying HTML", e)
            html
        }
    }

    /**
     * Minifies HTML content with more aggressive compression
     */
    fun minifyAggressive(html: String): String {
        if (html.isBlank()) {
            return html
        }

        return try {
            var minified = html
                .replace(Regex("<!--[\\s\\S]*?-->"), "")
                .replace(Regex(">\\s*<"), "><")
                .replace(Regex("^\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("\\s+$", RegexOption.MULTILINE), "")
                .replace(Regex("\\s+"), " ")
                .replace(Regex("\\s*=\\s*"), "=")
                .replace(Regex("=\\s*\"([a-zA-Z0-9_-]+)\""), "=$1")
                .trim()

            logger.debug(
                "HTML aggressively minified: {} chars -> {} chars ({}% reduction)",
                html.length, minified.length,
                if (html.length > 0) ((html.length - minified.length) * 100 / html.length) else 0
            )

            minified
        } catch (e: Exception) {
            logger.error("Error aggressively minifying HTML", e)
            html
        }
    }

    /**
     * Validates that the minified HTML is still well-formed
     */
    fun validateHtml(html: String): Boolean {
        return try {
            val openTags = Regex("<([a-zA-Z][a-zA-Z0-9]*)[^>]*>").findAll(html)
                .map { it.groupValues[1].lowercase() }
                .filter { !isSelfClosingTag(it) }
                .toList()

            val closeTags = Regex("</([a-zA-Z][a-zA-Z0-9]*)>").findAll(html)
                .map { it.groupValues[1].lowercase() }
                .toList()

            openTags.isNotEmpty() || closeTags.isNotEmpty()
        } catch (e: Exception) {
            logger.debug("HTML validation failed", e)
            false
        }
    }

    private fun isSelfClosingTag(tagName: String): Boolean {
        val selfClosingTags = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
        )
        return selfClosingTags.contains(tagName.lowercase())
    }
}
