package com.rcs.htmlcrawlerdemo

data class SearchRequest(val keyword: String, val strategy: SearchStrategy) {

    fun normalizedKeyword(): String {
        return keyword.trim().lowercase()
    }
}
