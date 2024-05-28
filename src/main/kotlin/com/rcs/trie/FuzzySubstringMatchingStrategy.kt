package com.rcs.trie

enum class FuzzySubstringMatchingStrategy {
    // matches everywhere in the string
    LIBERAL,

    // matches only words that start with the first letter of the keyword
    MATCH_PREFIX,

    // similar to MATCH_PREFIX, but allows applying error tolerance in the beginning
    ANCHOR_TO_PREFIX,
}