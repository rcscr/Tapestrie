package com.rcs.htmlcrawlerdemo

data class SearchRequest(
    val keyword: String,
    val strategy: SearchStrategy,
    val errorTolerance: Int?
) {

    fun normalizedKeyword(): String {
        return keyword.trim()
    }
}
