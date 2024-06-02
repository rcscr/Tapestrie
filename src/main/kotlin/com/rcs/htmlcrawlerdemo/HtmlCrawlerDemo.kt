package com.rcs.htmlcrawlerdemo

fun main() {
    // linux manual pages
    val baseUrl = "https://docs.huihoo.com/linux/man/20100621/"

    val htmlCrawler = HtmlCrawler(baseUrl, HtmlTokenizer(), HtmlUrlFinder(), HtmlClient())
    htmlCrawler.init()
    // Done initializing crawler; indexed 1860 HTML pages and 21181 unique tokens; took 90784 ms

    val searchRequest = SearchRequest("computer", SearchStrategy.FUZZY)
    val results = htmlCrawler.search(searchRequest)

    println(results)
}