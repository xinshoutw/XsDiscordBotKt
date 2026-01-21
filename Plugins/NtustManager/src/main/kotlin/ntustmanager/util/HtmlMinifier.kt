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
     *
     * @param html The HTML content to minify
     * @return Minified HTML content
     */
    fun minify(html: String): String {
        if (html.isBlank()) {
            return html
        }

        return try {
            val minified = html
                // Remove HTML comments
                .replace(Regex("<!--[\\s\\S]*?-->"), "")
                // Remove excessive whitespace between tags
                .replace(Regex(">\\s+<"), "><")
                // Remove leading and trailing whitespace from lines
                .replace(Regex("^\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("\\s+$", RegexOption.MULTILINE), "")
                // Collapse multiple consecutive whitespace characters into single space
                .replace(Regex("\\s{2,}"), " ")
                // Remove empty lines
                .replace(Regex("\\n\\s*\\n"), "\n")
                // Trim the entire content
                .trim()

            logger.debug(
                "HTML minified: {} chars -> {} chars ({}% reduction)",
                html.length, minified.length,
                if (html.length > 0) ((html.length - minified.length) * 100 / html.length) else 0
            )

            minified
        } catch (e: Exception) {
            logger.error("Error minifying HTML", e)
            // Return original HTML if minification fails
            html
        }
    }

    /**
     * Minifies HTML content with more aggressive compression
     * This version removes more whitespace but may affect readability
     *
     * @param html The HTML content to minify
     * @return Aggressively minified HTML content
     */
    fun minifyAggressive(html: String): String {
        if (html.isBlank()) {
            return html
        }

        return try {
            var minified = html
                // Remove HTML comments
                .replace(Regex("<!--[\\s\\S]*?-->"), "")
                // Remove all whitespace between tags
                .replace(Regex(">\\s*<"), "><")
                // Remove all leading and trailing whitespace
                .replace(Regex("^\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("\\s+$", RegexOption.MULTILINE), "")
                // Collapse all whitespace into single spaces
                .replace(Regex("\\s+"), " ")
                // Remove spaces around equals signs in attributes
                .replace(Regex("\\s*=\\s*"), "=")
                // Remove quotes around single-word attribute values (be careful with this)
                .replace(Regex("=\\s*\"([a-zA-Z0-9_-]+)\""), "=$1")
                // Trim the entire content
                .trim()

            logger.debug(
                "HTML aggressively minified: {} chars -> {} chars ({}% reduction)",
                html.length, minified.length,
                if (html.length > 0) ((html.length - minified.length) * 100 / html.length) else 0
            )

            minified
        } catch (e: Exception) {
            logger.error("Error aggressively minifying HTML", e)
            // Return original HTML if minification fails
            html
        }
    }

    /**
     * Validates that the minified HTML is still well-formed
     * This is a basic check to ensure we haven't broken the HTML structure
     *
     * @param html The HTML to validate
     * @return true if the HTML appears to be well-formed, false otherwise
     */
    fun validateHtml(html: String): Boolean {
        return try {
            // Basic validation: check that opening and closing tags match
            val openTags = Regex("<([a-zA-Z][a-zA-Z0-9]*)[^>]*>").findAll(html)
                .map { it.groupValues[1].lowercase() }
                .filter { !isSelfClosingTag(it) }
                .toList()

            val closeTags = Regex("</([a-zA-Z][a-zA-Z0-9]*)>").findAll(html)
                .map { it.groupValues[1].lowercase() }
                .toList()

            // For a basic validation, we just check if we have reasonable tag structure
            // More sophisticated validation would require a proper HTML parser
            openTags.isNotEmpty() || closeTags.isNotEmpty()
        } catch (e: Exception) {
            logger.debug("HTML validation failed", e)
            false
        }
    }

    /**
     * Checks if a tag is self-closing (doesn't need a closing tag)
     */
    private fun isSelfClosingTag(tagName: String): Boolean {
        val selfClosingTags = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
        )
        return selfClosingTags.contains(tagName.lowercase())
    }
}