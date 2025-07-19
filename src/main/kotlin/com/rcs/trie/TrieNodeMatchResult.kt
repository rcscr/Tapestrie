package com.rcs.trie

data class TrieNodeMatchResult(
    val exactMatch: Boolean,
    val caseInsensitiveMatch: Boolean? = null,
    val diacriticInsensitiveMatch: Boolean? = null,
    val caseAndDiacriticInsensitiveMatch: Boolean? = null,
) {
    val anyMatch: Boolean = exactMatch
            || caseInsensitiveMatch == true
            || diacriticInsensitiveMatch == true
            || caseAndDiacriticInsensitiveMatch == true
}