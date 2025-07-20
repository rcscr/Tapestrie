package com.rcs.trie

data class TrieSearchResult<T>(
    // the data stored in the trie (not the keyword being searched)
    val string: String,
    // the value associated with the data
    val value: T,
    // statistics regarding the match
    val stats: TrieSearchResultStats
)