package com.rcs.htmlcrawlerdemo

import com.rcs.trie.FuzzySubstringMatchingStrategy
import com.rcs.trie.Trie
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class HtmlCrawler(
    private val baseUrl: String,
    private val htmlTokenizer: HtmlTokenizer,
    private val htmlUrlFinder: HtmlUrlFinder,
    private val htmlClient: HtmlClient
) {

    private val trie: Trie<MutableSet<String>> = Trie() // maps a token to a set of URLs where the token can be found

    private var initialized = false

    fun init() {
        println("Initializing crawler with baseURL=${this.baseUrl}")

        val currentTimeMillis = System.currentTimeMillis()

        var counts: Pair<Int, Int>

        // synchronization prevents searching while crawling/indexing
        synchronized(this.trie) {
            trie.clear()
            counts = crawl("", ConcurrentHashMap())
        }

        val durationMillis = System.currentTimeMillis() - currentTimeMillis

        println("Done initializing crawler; " +
                "indexed ${counts.first} HTML pages and ${counts.second} unique tokens; took $durationMillis ms")

        this.initialized = true
    }

    fun search(searchRequest: SearchRequest): List<String> {
        if (!initialized) {
            throw IllegalStateException("Crawler has not been initialized; call HtmlCrawler.init() first")
        }

        val normalizedKeyword: String = searchRequest.normalizedKeyword()

        var resultsWithoutBaseUrl: Collection<String>

        // synchronization prevents searching while crawling/indexing
        synchronized(this.trie) {
            resultsWithoutBaseUrl = when(searchRequest.strategy) {
                SearchStrategy.EXACT -> {
                    trie.getExactly(normalizedKeyword) ?: setOf()
                }
                SearchStrategy.SUBSTRING -> {
                    trie.matchBySubstring(normalizedKeyword)
                        .flatMap { it.value }
                }
                SearchStrategy.FUZZY -> {
                    // for this large data set, it's better to limit the
                    // fuzzy strategy to the more restrictive MATCH_PREFIX
                    // other strategies will still work, but will be slow
                    trie.matchBySubstringFuzzy(normalizedKeyword, 2, FuzzySubstringMatchingStrategy.MATCH_PREFIX)
                        .flatMap { it.value }
                }
            }
        }

        return resultsWithoutBaseUrl
            .map { relativeUrl: String -> baseUrl + relativeUrl }
            .toList()
    }

    /**
     * returns
     * Pair.first = number of pages indexed
     * Pair.second = number of unique words indexed
     */
    private fun crawl(relativeUrl: String, visited: ConcurrentHashMap<String, Boolean?>): Pair<Int, Int> {
        // Use putIfAbsent to check and mark the URL atomically
        if (visited.putIfAbsent(relativeUrl, true) != null) {
            return Pair(0, 0) // URL already visited
        }

        val htmlContent: String
        try {
            htmlContent = htmlClient.getAsString(baseUrl + relativeUrl)
        } catch (e: IOException) {
            println("Error fetching ${baseUrl + relativeUrl} - not indexing page")
            return Pair(0, 0)
        }

        val wordsIndexed = indexPage(relativeUrl, htmlContent)

        val newCounts = htmlUrlFinder.findRelativeUrls(htmlContent)
            .parallelStream()
            .map { u -> fixUrl("/$relativeUrl", u) }
            .map { u -> crawl(u, visited) }
            .reduce(Pair(0, 0)) { a, b ->
                Pair(Integer.sum(a.first, b.first), Integer.sum(a.second, b.second))
            }

        return Pair(1 + newCounts.first, wordsIndexed + newCounts.second)
    }

    private fun fixUrl(relativeUrl: String, additionalPath: String): String {
        var fixedUrl = additionalPath
        var prefixUrl = relativeUrl.substring(0, relativeUrl.lastIndexOf("/"))
        while (fixedUrl.startsWith("../")) {
            fixedUrl = fixedUrl.replace("../", "")
            prefixUrl = prefixUrl.substring(0, prefixUrl.lastIndexOf("/"))
        }
        return prefixUrl + fixedUrl
    }

    private fun indexPage(relativeUrl: String, htmlContent: String): Int {
        println("Indexing ${baseUrl + relativeUrl}")

        var wordsIndex = 0

        htmlTokenizer.tokenize(htmlContent)
            .forEach { token ->
                val newKeys: MutableSet<String> = trie.getExactly(token) ?: mutableSetOf()
                if (newKeys.isEmpty()) {
                    wordsIndex++
                }
                // stores only relative URLs in order to minimize storage space
                // the full URL must then be reconstructed on retrieval!
                newKeys.add(relativeUrl)
                trie.put(token, newKeys)
            }

        return wordsIndex
    }
}