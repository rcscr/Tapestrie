package com.rcs.trie

data class TrieSearchResult<T>(
    val string: String,
    val value: T,
    val lengthOfMatch: Int,
    val errors: Int,
    val prefixDistance: Int,
    val matchedWholeSequence: Boolean,
    val matchedWholeWord: Boolean
)