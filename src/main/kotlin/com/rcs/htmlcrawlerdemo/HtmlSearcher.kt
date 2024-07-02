package com.rcs.htmlcrawlerdemo

import com.rcs.trie.FuzzyMatchingStrategy
import com.rcs.trie.Trie
import com.rcs.trie.TrieSearchResult
import java.util.concurrent.ConcurrentLinkedDeque

class HtmlSearcher(private val baseUrl: String, private val htmlCrawler: HtmlCrawler) {

    // maps a token to a set of URLs where the token can be found
    private lateinit var trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>>

    private var initialized = false

    private val crawlingIndexingLock = Any()

    fun crawlAndIndex() {
        synchronized(crawlingIndexingLock) {
            trie = htmlCrawler.crawlAndIndex()
        }
        initialized = true
    }

    fun search(searchRequest: SearchRequest): List<TrieSearchResult<List<HtmlIndexEntry>>> {
        if (!initialized) {
            throw IllegalStateException("HtmlSearcher has not been initialized; " +
                    "call HtmlSearcher.crawlAndIndex() first")
        }

        val normalizedKeyword = searchRequest.normalizedKeyword()

        var resultsWithoutBaseUrl: Collection<TrieSearchResult<ConcurrentLinkedDeque<HtmlIndexEntry>>>

        // synchronization prevents searching while crawling/indexing
        synchronized(crawlingIndexingLock) {
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
                        FuzzyMatchingStrategy.LIBERAL)
                }
            }
        }

        return resultsWithoutBaseUrl
            .map { enrichWithBaseUrl(it) }
            .toList()
    }

    private fun emulatedTrieSearchResult(
        string: String,
        value: ConcurrentLinkedDeque<HtmlIndexEntry>
    ): TrieSearchResult<ConcurrentLinkedDeque<HtmlIndexEntry>> {
        return TrieSearchResult(string, value, string, string, string.length, 0, 0, true, true)
    }

    private fun enrichWithBaseUrl(
        result: TrieSearchResult<ConcurrentLinkedDeque<HtmlIndexEntry>>
    ): TrieSearchResult<List<HtmlIndexEntry>> {
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