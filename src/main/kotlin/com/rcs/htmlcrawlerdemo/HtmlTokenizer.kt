package com.rcs.htmlcrawlerdemo

import java.util.*

class HtmlTokenizer {

    /**
     * returns a map of a token to its number of occurrences
     */
    fun tokenize(htmlContent: String): Map<String, HtmlTokenInfo> {
        // Remove HTML tags using a regular expression
        val noHtml = htmlContent.replace("<[^>]*>".toRegex(), " ")

        // Remove non-alphanumeric characters and extra space
        val cleaned = noHtml.replace("[^a-zA-Z0-9\\s]".toRegex(), " ")

        // Normalize multiple spaces to a single space
        val singleSpace = cleaned.replace("\\s+".toRegex(), " ")

        // Convert to lowercase
        val lowerCase = singleSpace.lowercase(Locale.getDefault())

        // Split
        val tokens = lowerCase.split(" ").toTypedArray()

        // Remove blanks, and collect as a histogram
        return tokens
            .filter { it.isNotBlank() }
            .foldIndexed(mutableMapOf()) { index, map, next ->
                val indexOfNext = index + 1
                val existingInfo = map[next]
                map[next] = HtmlTokenInfo(
                    occurrences = (existingInfo?.occurrences ?: 0) + 1,
                    context = (existingInfo?.context ?: mutableListOf()) + getContext(tokens, indexOfNext)
                )
                map
            }
    }

    private fun getContext(tokens: Array<String>, index: Int): String {
        val startInclusive = Math.max(0, index - 2)
        val endExclusive = Math.min(tokens.size, index + 3)
        return tokens.copyOfRange(startInclusive, endExclusive).joinToString(" ")
    }
}