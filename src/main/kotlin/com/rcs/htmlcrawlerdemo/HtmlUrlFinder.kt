package com.rcs.htmlcrawlerdemo

import java.util.HashSet
import java.util.regex.Pattern

class HtmlUrlFinder {

    companion object {
        private const val relativeHrefRegex =
            "<a\\s+[^>]*href\\s*=\\s*\"(?!http|https|mailto|ftp)([^\"]*\\.html)\""

        private val relativeHrefPattern =
            Pattern.compile(relativeHrefRegex, Pattern.CASE_INSENSITIVE)
    }

    fun findRelativeUrls(url: String, htmlContent: String): Set<String> {
        val matcher = relativeHrefPattern.matcher(htmlContent)
        val links: MutableSet<String> = HashSet()
        while (matcher.find()) {
            val link = matcher.group(1)
            links.add(link)
        }
        return links.map { makeFullUrl(url, it) }.toSet()
    }

    private fun makeFullUrl(baseUrl: String, additionalPath: String): String {
        var fixedBaseUrl = baseUrl
        var fixedAdditionalUrl = additionalPath

        // remove any file name from base URL
        fixedBaseUrl = fixedBaseUrl.substring(0, fixedBaseUrl.lastIndexOf("/"))

        // remove any possible first / for consistency
        if (fixedAdditionalUrl.startsWith("/")) {
            fixedAdditionalUrl = fixedAdditionalUrl.replaceFirst("/", "")
        }

        // file in same directory
        if (fixedAdditionalUrl.startsWith("./")) {
            fixedAdditionalUrl = fixedAdditionalUrl.replaceFirst("./", "")
        }

        // if additionalUrl contains a ../ in the middle,
        // remove it and make the appropriate changes in the same variable
        val index = fixedAdditionalUrl.indexOf("../")
        if (index > 1) {
            fixedAdditionalUrl = fixedAdditionalUrl.substring(index + 3, fixedAdditionalUrl.length)
        }

        // if additionalUrl contains a ../ in the start,
        // remove it and make the appropriate changes in fixedBaseUrl
        while (fixedAdditionalUrl.startsWith("../")) {
            fixedAdditionalUrl = fixedAdditionalUrl.replace("../", "")
            fixedBaseUrl = fixedBaseUrl.substring(0, fixedBaseUrl.lastIndexOf("/"))
        }

        return ("$fixedBaseUrl/$fixedAdditionalUrl")
    }
}