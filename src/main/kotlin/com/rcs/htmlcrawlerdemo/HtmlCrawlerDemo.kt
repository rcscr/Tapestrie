package com.rcs.htmlcrawlerdemo

fun main() {
    val baseUrl = "to be configured at run time"
    val htmlCrawler = HtmlCrawler(baseUrl, HtmlTokenizer(), HtmlUrlFinder(), HtmlClient())
    htmlCrawler.init()

    // this is a great use-case because "indices" and "indexes" are both acceptable spellings
    val searchRequest = SearchRequest("indices", SearchStrategy.FUZZY)
    val results = htmlCrawler.search(searchRequest)

    // in the beginning of the list, you will find the exact match "indices" with 0 errors
    // then "indic" and "indexes" with 2 errors - fine
    // then we have less relevant hits like "induced" and "indicate"
    // all the way to "indistinguishable"! why? Because "indices" match "indis" minus "ce", which falls within the error allowance
    // clearly, the algorithm works as intended, but more work needs to be done to filter out irrelevant results
    println(results)
}