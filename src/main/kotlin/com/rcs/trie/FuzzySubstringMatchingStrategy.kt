package com.rcs.trie

enum class FuzzySubstringMatchingStrategy {
    // matches everywhere in the string, and allows errors in the beginning, middle, and end
    LIBERAL,

    // matches only words that start with the first letter of the keyword
    EXACT_PREFIX,

    // similar to EXACT_PREFIX, but allows applying error tolerance in the beginning
    FUZZY_PREFIX,

    // matches everywhere in the string, but allows errors only at the end
    FUZZY_POSTFIX
}