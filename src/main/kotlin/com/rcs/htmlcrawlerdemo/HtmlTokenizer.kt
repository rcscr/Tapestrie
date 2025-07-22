package com.rcs.htmlcrawlerdemo

import com.rcs.trie.FuzzySearchUtils.Companion.indexOfFirstWordSeparator
import com.rcs.trie.FuzzySearchUtils.Companion.indexOfLastWordSeparator
import java.util.*

class HtmlTokenizer {

    /**
     * returns a map of a token to its number of occurrences
     */
    fun tokenize(htmlContent: String): Map<String, HtmlTokenInfo> {
        val normalized = htmlContent
            .trim()
            // Remove HTML tags
            .replace("<[^>]*>".toRegex(), " ")
            // Remove new line characters and similar useless stuff
            .replace("[\\r\\n\\f\\v\\u2028\\u2029\\u00A0]+".toRegex(), " ")
            // Normalize multiple spaces to a single space
            .replace("\\s+".toRegex(), " ")

        val map = mutableMapOf<String, HtmlTokenInfo>()

        var index = 0

        while (index < normalized.length) {
            val nextWordSeparatorIndex = normalized.indexOfFirstWordSeparator(index) ?: normalized.length

            val token = normalized
                .substring(index, nextWordSeparatorIndex)
                .lowercase(Locale.getDefault())

            if (token.isNotBlank()) {
                map[token] = HtmlTokenInfo(
                    occurrences = (map[token]?.occurrences ?: 0) + 1,
                    context = (map[token]?.context ?: mutableListOf()) + getContext(normalized, index, nextWordSeparatorIndex)
                )
            }

            index = nextWordSeparatorIndex + 1
        }

        return map
    }

    private fun getContext(content: String, index: Int, nextWordSeparatorIndex: Int): String {
        return content.substring(
            content.indexOfLastWordSeparator(index - 20)?.let { it + 1 } ?: 0,
            content.indexOfFirstWordSeparator(nextWordSeparatorIndex + 20) ?: content.length
        )
    }
}