package com.rcs.htmlcrawlerdemo

import com.rcs.trie.Trie
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

class HtmlCrawler(
    private val baseUrl: String,
    private val htmlTokenizer: HtmlTokenizer,
    private val htmlUrlFinder: HtmlUrlFinder,
    private val htmlClient: HtmlClient
) {

    fun crawlAndIndex(): Trie<ConcurrentLinkedDeque<HtmlIndexEntry>> = runBlocking {
        println("Initializing crawler with baseURL=${baseUrl}")

        val currentTimeMillis = System.currentTimeMillis()

        val trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>> = Trie()

        val pagesIndexed = crawl(baseUrl, ConcurrentHashMap(), trie)

        val durationMillis = System.currentTimeMillis() - currentTimeMillis

        println("Done initializing crawler; " +
                "indexed $pagesIndexed HTML pages and ${trie.size} unique tokens; " +
                "took ${durationMillis}ms")

        trie
    }

    private suspend fun crawl(
        url: String,
        visited: ConcurrentHashMap<String, Boolean?>,
        trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>>
    ): Int = coroutineScope {
        // Use putIfAbsent to check and mark the URL atomically
        if (visited.putIfAbsent(url, true) != null) {
            return@coroutineScope 0 // URL already visited
        }

        val htmlContent: String
        try {
            htmlContent = htmlClient.getAsString(url)
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error fetching $url - not indexing page")
            return@coroutineScope 0
        }

        indexPage(url, htmlContent, trie)

        val newCounts = htmlUrlFinder.findRelativeUrls(url, htmlContent)
            .map { u -> async(Dispatchers.IO) { crawl(u, visited, trie) } }
            .sumOf { it.await() }

        1 + newCounts
    }

    private fun indexPage(url: String, htmlContent: String, trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>>) {
        println("Indexing $url")

        // stores only relative URLs in order to minimize storage space
        // the full URL must then be reconstructed on retrieval!
        val relativeUrl = url.substring(baseUrl.length, url.length)

        htmlTokenizer.tokenize(htmlContent)
            .forEach { entry ->
                val token = entry.key
                val occurrences = entry.value
                val indexEntries = trie.getExactly(token) ?: ConcurrentLinkedDeque()
                val newEntry = HtmlIndexEntry(relativeUrl, occurrences)
                indexEntries.add(newEntry)
                indexEntries.sortedBy { it.tokenInfo.occurrences }
                trie.put(token, indexEntries)
            }
    }
}