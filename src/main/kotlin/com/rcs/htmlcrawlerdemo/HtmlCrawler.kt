package com.rcs.htmlcrawlerdemo

import com.rcs.trie.FuzzySubstringMatchingStrategy
import com.rcs.trie.Trie
import com.rcs.trie.TrieSearchResult
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService

class HtmlCrawler(
    private val baseUrl: String,
    private val htmlTokenizer: HtmlTokenizer,
    private val htmlUrlFinder: HtmlUrlFinder,
    private val htmlClient: HtmlClient,
    private val executorService: ExecutorService
) {

    // maps a token to a set of URLs where the token can be found
    private val trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>> = Trie()

    private var initialized = false

    private var crawlingLock = Any()

    fun init() {
        println("Initializing crawler with baseURL=${this.baseUrl}")

        val currentTimeMillis = System.currentTimeMillis()

        var pagesIndexed: Int

        // synchronization prevents searching while crawling/indexing
        synchronized(crawlingLock) {
            trie.clear()
            pagesIndexed = crawl("", ConcurrentHashMap())
        }

        val durationMillis = System.currentTimeMillis() - currentTimeMillis

        println("Done initializing crawler; " +
                "indexed $pagesIndexed HTML pages and ${trie.size} unique tokens; " +
                "took ${durationMillis}ms")

        this.initialized = true
    }

    fun search(searchRequest: SearchRequest): List<TrieSearchResult<List<HtmlIndexEntry>>> {
        if (!initialized) {
            throw IllegalStateException("Crawler has not been initialized; call HtmlCrawler.init() first")
        }

        val normalizedKeyword = searchRequest.normalizedKeyword()

        var resultsWithoutBaseUrl: Collection<TrieSearchResult<ConcurrentLinkedDeque<HtmlIndexEntry>>>

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
                        searchRequest.errorTolerance!!,
                        FuzzySubstringMatchingStrategy.LIBERAL)
                }
            }
        }

        return resultsWithoutBaseUrl
            .map { enrichWithBaseUrl(it) }
            .toList()
    }

    private fun crawl(relativeUrl: String, visited: ConcurrentHashMap<String, Boolean?>): Int {
        // Use putIfAbsent to check and mark the URL atomically
        if (visited.putIfAbsent(relativeUrl, true) != null) {
            return 0 // URL already visited
        }

        val htmlContent: String
        try {
            htmlContent = htmlClient.getAsString(baseUrl + relativeUrl)
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error fetching ${baseUrl + relativeUrl} - not indexing page")
            return 0
        }

        indexPage(relativeUrl, htmlContent)

        val newCounts = htmlUrlFinder.findRelativeUrls(htmlContent)
            .map { u -> fixUrl("/$relativeUrl", u) }
            .map { u -> executorService.submit<Int> { crawl(u, visited) } }
            .sumOf { it.get() }

        return 1 + newCounts
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

    private fun indexPage(relativeUrl: String, htmlContent: String) {
        println("Indexing ${baseUrl + relativeUrl}")

        htmlTokenizer.tokenize(htmlContent)
            .forEach { entry ->
                val token = entry.key
                val occurrences = entry.value
                val indexEntries = trie.getExactly(token) ?: ConcurrentLinkedDeque()
                // stores only relative URLs in order to minimize storage space
                // the full URL must then be reconstructed on retrieval!
                val newEntry = HtmlIndexEntry(relativeUrl, occurrences)
                indexEntries.add(newEntry)
                indexEntries.sortedBy { it.occurrences }
                trie.put(token, indexEntries)
            }
    }

    private fun emulatedTrieSearchResult(string: String, value: ConcurrentLinkedDeque<HtmlIndexEntry>): TrieSearchResult<ConcurrentLinkedDeque<HtmlIndexEntry>> {
        return TrieSearchResult(string, value, string, string, string.length, 0, 0, true, true)
    }

    private fun enrichWithBaseUrl(result: TrieSearchResult<ConcurrentLinkedDeque<HtmlIndexEntry>>): TrieSearchResult<List<HtmlIndexEntry>> {
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