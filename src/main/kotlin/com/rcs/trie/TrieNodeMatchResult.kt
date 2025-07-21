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

    val caseInsensitiveCount: Int = when(
        !exactMatch && (caseInsensitiveMatch == true || (diacriticInsensitiveMatch == false && caseAndDiacriticInsensitiveMatch == true))
    ) {
        true -> 1
        else -> 0
    }

    val diacriticInsensitiveCount: Int = when(
        !exactMatch && (diacriticInsensitiveMatch == true || (caseInsensitiveMatch == false && caseAndDiacriticInsensitiveMatch == true))
    ) {
        true -> 1
        else -> 0
    }
}