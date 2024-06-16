package com.rcs.htmlcrawlerdemo

import java.util.HashSet
import java.util.regex.Pattern

class HtmlUrlFinder {

    companion object {
        private val relativeHrefRegex =
            "<a\\s+[^>]*href\\s*=\\s*\"(?!http|https|mailto|ftp)([^\"]*\\.html)\""

        private val relativeHrefPattern =
            Pattern.compile(relativeHrefRegex, Pattern.CASE_INSENSITIVE)
    }

    fun findRelativeUrls(htmlContent: String): Set<String> {
        val matcher = relativeHrefPattern.matcher(htmlContent)
        val links: MutableSet<String> = HashSet()
        while (matcher.find()) {
            val link = matcher.group(1)
            links.add(link)
        }
        return links
    }
}