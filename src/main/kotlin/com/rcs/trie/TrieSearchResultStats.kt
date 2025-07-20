package com.rcs.trie

/**
 * For all fields in this class, there should be a corresponding
 * comparator in TrieSearchResultComparator.
 */
data class TrieSearchResultStats(
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

    // if caseInsensitive, number of case mismatches (which are not counted as errors)
    val numberOfCaseMismatches: Int,

    // if diacriticInsensitive, number of diacritic mismatches (which are not counted as errors)
    val numberOfDiacriticMismatches: Int,

    // the distance from the start of the match to the beginning of the word
    val prefixDistance: Int,

    // whether the keyword perfectly matched the entire string
    val matchedWholeString: Boolean,

    // whether the keyword perfectly matched a whole word(s) within the string
    val matchedWholeWord: Boolean
)