package com.rcs.htmlcrawlerdemo

fun main() {
    // linux manual pages
    val baseUrl = "https://docs.huihoo.com/linux/man/20100621/"

    val htmlCrawler = HtmlCrawler(baseUrl, HtmlTokenizer(), HtmlUrlFinder(), HtmlClient())
    htmlCrawler.init()
    // Done initializing crawler; indexed 1860 HTML pages and 21181 unique tokens; took 90784 ms

    // this is a great use-case because "indices" and "indexes" are both acceptable spellings
    val searchRequest = SearchRequest("indices", SearchStrategy.FUZZY)
    val results = htmlCrawler.search(searchRequest)

    // in the beginning of the list, you will find the exact match "indices" with 0 errors
    // then "indic" and "indexes" with 2 errors - fine
    // then we have less relevant hits like "induced" and "indicate"
    println(results)
}