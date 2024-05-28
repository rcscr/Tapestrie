package com.rcs.htmlcrawlerdemo

import java.util.*

class HtmlTokenizer {

    /**
     * returns a map of a token to its number of occurrences
     */
    fun tokenize(htmlContent: String): Map<String, Int> {
        // Remove HTML tags using a regular expression
        val noHtml = htmlContent.replace("<[^>]*>".toRegex(), " ")

        // Remove non-alphanumeric characters and extra space
        val cleaned = noHtml.replace("[^a-zA-Z0-9\\s]".toRegex(), " ")

        // Normalize multiple spaces to a single space
        val singleSpace = cleaned.replace("\\s+".toRegex(), " ")

        // Convert to lowercase
        val lowerCase = singleSpace.lowercase(Locale.getDefault())

        // Split, remove blanks, and remove duplicates (by converting to Set)
        return lowerCase.split(" ")
            .filter { it.isNotBlank() }
            .fold(mutableMapOf()) { map, next ->
                map[next] = (map[next] ?: 0) + 1
                map
            }
    }
}