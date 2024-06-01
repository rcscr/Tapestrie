package com.rcs.trie

fun main() {
    val trie = Trie<Unit>()

    trie.put("this is rafael", Unit)

    // results in two duplicate search results!
    trie.matchBySubstringFuzzy("raphael", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        .forEach { println(it) }
}