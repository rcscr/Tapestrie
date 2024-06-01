package com.rcs.trie

fun main() {
    val trie = Trie<Unit>()
    trie.put("lalala0 lalala1 lalala2 lalala3", Unit)

    trie.matchBySubstringFuzzy("lalala2", 2, FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX)
        .forEach { println(it) }
}