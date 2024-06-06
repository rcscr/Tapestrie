package com.rcs.htmlcrawlerdemo

import com.rcs.trie.FuzzySubstringMatchingStrategy
import com.rcs.trie.Trie
import com.rcs.trie.TrieSearchResult
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

private data class IndexCounts(val pages: Int, val uniqueTokens: Int) {

    fun add(other: IndexCounts): IndexCounts {
        return IndexCounts(pages + other.pages, uniqueTokens + other.uniqueTokens)
    }
}

class HtmlCrawler(
    private val baseUrl: String,
    private val htmlTokenizer: HtmlTokenizer,
    private val htmlUrlFinder: HtmlUrlFinder,
    private val htmlClient: HtmlClient,
    private val executorService: ExecutorService
) {

    // maps a token to a set of URLs where the token can be found
    private val trie: Trie<LinkedList<HtmlIndexEntry>> = Trie()

    private var initialized = false

    private var crawlingLock = Any()

    fun init() {
        println("Initializing crawler with baseURL=${this.baseUrl}")

        val currentTimeMillis = System.currentTimeMillis()

        var counts: IndexCounts

        // synchronization prevents searching while crawling/indexing
        synchronized(crawlingLock) {
            trie.clear()
            counts = crawl("", ConcurrentHashMap())
        }

        val durationMillis = System.currentTimeMillis() - currentTimeMillis

        println("Done initializing crawler; " +
                "indexed ${counts.pages} HTML pages and ${counts.uniqueTokens} unique tokens; " +
                "took ${durationMillis}ms")

        this.initialized = true
    }

    fun search(searchRequest: SearchRequest): List<TrieSearchResult<List<HtmlIndexEntry>>> {
        if (!initialized) {
            throw IllegalStateException("Crawler has not been initialized; call HtmlCrawler.init() first")
        }

        val normalizedKeyword = searchRequest.normalizedKeyword()

        var resultsWithoutBaseUrl: Collection<TrieSearchResult<LinkedList<HtmlIndexEntry>>>

        // synchronization prevents searching while crawling/indexing
        synchronized(crawlingLock) {
            resultsWithoutBaseUrl = when(searchRequest.strategy) {
                SearchStrategy.EXACT -> {
                    trie.getExactly(normalizedKeyword)
                        ?.let { setOf(emulatedTrieSearchResult(normalizedKeyword, it)) }
                        ?: setOf()
                }
                SearchStrategy.SUBSTRING -> {
                    trie.matchBySubstring(normalizedKeyword)
                }
                SearchStrategy.FUZZY -> {
                    trie.matchBySubstringFuzzy(
                        normalizedKeyword,
                        searchRequest.errorTolerance,
                        FuzzySubstringMatchingStrategy.LIBERAL)
                }
            }
        }

        return resultsWithoutBaseUrl
            .map { enrichWithBaseUrl(it) }
            .toList()
    }

    private fun crawl(relativeUrl: String, visited: ConcurrentHashMap<String, Boolean?>): IndexCounts {
        // Use putIfAbsent to check and mark the URL atomically
        if (visited.putIfAbsent(relativeUrl, true) != null) {
            return IndexCounts(0, 0) // URL already visited
        }

        val htmlContent: String
        try {
            htmlContent = htmlClient.getAsString(baseUrl + relativeUrl)
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error fetching ${baseUrl + relativeUrl} - not indexing page")
            return IndexCounts(0, 0)
        }

        val uniqueTokens = indexPage(relativeUrl, htmlContent)

        val newCounts = htmlUrlFinder.findRelativeUrls(htmlContent)
            .map { u -> fixUrl("/$relativeUrl", u) }
            .map { u -> executorService.submit<IndexCounts> { crawl(u, visited) } }
            .map { it.get() }
            .fold(IndexCounts(0, 0)) { result, next -> result.add(next) }

        return IndexCounts(1 + newCounts.pages, uniqueTokens + newCounts.uniqueTokens)
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

        var newTokensIndexed = 0

        htmlTokenizer.tokenize(htmlContent)
            .forEach { entry ->
                val token = entry.key
                val occurrences = entry.value

                synchronized(token) {
                    val indexEntries = trie.getExactly(token) ?: LinkedList()

                    synchronized(indexEntries) {
                        if (indexEntries.isEmpty()) {
                            newTokensIndexed++
                        }

                        // stores only relative URLs in order to minimize storage space
                        // the full URL must then be reconstructed on retrieval!
                        val newEntry = HtmlIndexEntry(relativeUrl, occurrences)
                        indexEntries.add(newEntry)

                        indexEntries.sortByDescending { it.occurrences }

                        trie.put(token, indexEntries)
                    }
                }
            }

        return newTokensIndexed
    }

    private fun emulatedTrieSearchResult(string: String, value: LinkedList<HtmlIndexEntry>): TrieSearchResult<LinkedList<HtmlIndexEntry>> {
        return TrieSearchResult(string, value, string, string, string.length, 0, 0, true, true)
    }

    private fun enrichWithBaseUrl(result: TrieSearchResult<LinkedList<HtmlIndexEntry>>): TrieSearchResult<List<HtmlIndexEntry>> {
        return TrieSearchResult(
            result.string,
            result.value.map { HtmlIndexEntry(baseUrl + it.url, it.occurrences) },
            result.matchedSubstring,
            result.matchedWord,
            result.numberOfMatches,
            result.numberOfErrors,
            result.prefixDistance,
            result.matchedWholeString,
            result.matchedWholeWord
        )
    }
}