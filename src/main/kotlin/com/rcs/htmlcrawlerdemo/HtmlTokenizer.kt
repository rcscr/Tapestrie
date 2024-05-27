package com.rcs.htmlcrawlerdemo

import java.util.*

class HtmlTokenizer {

    fun tokenize(htmlContent: String): Set<String> {
        // Remove HTML tags using a regular expression
        val noHtml = htmlContent.replace("<[^>]*>".toRegex(), " ")

        // Remove non-alphanumeric characters and extra space
        val cleaned = noHtml.replace("[^a-zA-Z0-9\\s]".toRegex(), " ")

        // Normalize multiple spaces to a single space
        val singleSpace = cleaned.replace("\\s+".toRegex(), " ").trim { it <= ' ' }

        // Convert to lowercase
        val lowerCase = singleSpace.lowercase(Locale.getDefault())

        // Split and remove duplicates (Set)
        return lowerCase.split(" ").toSet()
    }
}