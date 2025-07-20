package com.rcs.trie

enum class FuzzyMatchingStrategy {
    // matches everywhere in the string, and allows errors in the beginning, middle, and end
    LIBERAL,

    // matches only words that start with the first letter of the keyword, regardless of error tolerance
    EXACT_PREFIX,

    // similar to EXACT_PREFIX, but allows the error tolerance to be applies at the beginning (not in the middle or end)
    FUZZY_PREFIX,

    // similar to EXACT_PREFIX, but allows the error tolerance only at the end (not in the beginning or middle)
    FUZZY_POSTFIX,

    // accepts only errors due to adjacent letter swaps (i.e. typos)
    ADJACENT_SWAP,

    // accepts only errors due to letter swaps anywhere in the string
    SYMMETRICAL_SWAP,

    // matches strings containing words that form the acronym provided
    ACRONYM
}