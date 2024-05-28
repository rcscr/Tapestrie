package com.rcs.trie

data class TrieSearchResult<T>(
    // the data store in the trie (not the keyword being searched)
    val string: String,

    // the value associated with the data
    val value: T,

    // the minimum portion of the string that matched the keyword, including errors
    val matchSubstring: String,

    // number of characters that matched
    val lengthOfMatch: Int,

    // number of characters that didn't match due to misspelling or letters missing
    val errors: Int,

    // the distance from the start of the match to the beginning of the word
    val prefixDistance: Int,

    // the length of the word where the match was found
    val wordLength: Int,

    // whether the keyword perfectly matched the entire string stored in the Trie
    val matchedWholeSequence: Boolean,

    // whether the keyword perfectly matched a whole word
    val matchedWholeWord: Boolean
)