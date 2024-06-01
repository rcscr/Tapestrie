package com.rcs.trie

fun main() {
    /**
    FuzzySearchScenario(
    "MatchingStrategy=ANCHOR_TO_PREFIX only matches beginning of word with errorTolerance=2",
    setOf("lalaindex", "index", "ondex", "oldex", "omtex", "lalala index", "lalala ondex", "lalala oldex", "lalala omtex"),
    "index",
    2,
    FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX,
    listOf(
    TrieSearchResult("index", Unit, "index", "index", 5, 0, 0, true, true),
    TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
    TrieSearchResult("ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
    TrieSearchResult("lalala ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
    TrieSearchResult("oldex", Unit, "dex", "oldex", 3, 2, 2, false, false),
    TrieSearchResult("lalala oldex", Unit, "dex", "oldex", 3, 2, 2, false, false)
    )
    )
     */
    val trie = Trie<Unit>()

//    setOf("lalaindex", "index", "ondex", "oldex", "omtex", "lalala index", "lalala ondex", "lalala oldex", "lalala omtex")
//        .forEach { trie.put(it, Unit) }
    trie.put("lalaindex", Unit)
    trie.put("index", Unit)

    trie.matchBySubstringFuzzy("index", 2, FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX)
        .forEach { println(it) }
}