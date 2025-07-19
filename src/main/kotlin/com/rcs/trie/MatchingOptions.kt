package com.rcs.trie

data class MatchingOptions(
    val caseInsensitive: Boolean,
    val diacriticInsensitive: Boolean,
) {
    companion object {
        val allDisabled = MatchingOptions(
            caseInsensitive = false,
            diacriticInsensitive = false
        )
    }
}