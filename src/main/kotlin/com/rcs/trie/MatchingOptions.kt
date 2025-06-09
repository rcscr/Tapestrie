package com.rcs.trie

class MatchingOptions(
    val caseInsensitive: Boolean,
    val diacriticInsensitive: Boolean,
) {
    companion object {
        val default = MatchingOptions(
            caseInsensitive = false,
            diacriticInsensitive = false
        )
    }
}