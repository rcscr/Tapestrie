package com.rcs.trie

enum class FuzzySubstringMatchingStrategy {
    LIBERAL, // matches everywhere in the string
    MATCH_PREFIX, // matches only words that start with the first letter of the keyword
    ANCHOR_TO_PREFIX, // similar to MATCH_PREFIX, but allows applying error tolerance in the beginning
}