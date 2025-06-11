package com.rcs.trie

data class TrieSearchResult<T>(
    // the data stored in the trie (not the keyword being searched)
    val string: String,

    // the value associated with the data
    val value: T,

    // the minimum portion of the string that matched the keyword,
    // including errors in between (but not errors before or after)
    val matchedSubstring: String,

    // the whole word(s) where the match was found
    // useful if the data in the trie is composed of multiple words
    val matchedWord: String,

    // number of characters that matched
    val numberOfMatches: Int,

    // number of errors
    val numberOfErrors: Int,

    // the distance from the start of the match to the beginning of the word
    val prefixDistance: Int,

    // whether the keyword perfectly matched the entire string
    val matchedWholeString: Boolean,

    // whether the keyword perfectly matched a whole word(s) within the string
    val matchedWholeWord: Boolean
)