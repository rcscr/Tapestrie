package com.rcs.trie

import kotlin.test.Test
import com.rcs.trie.FuzzyMatchingStrategy.*
import com.rcs.trie.utils.TestUtils.FuzzySearchScenario
import com.rcs.trie.utils.TestUtils.Companion.runTestScenario

class TrieFuzzySearchTest {

    @Test
    fun `all matching strategies work as expected when caseInsensitive = true`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX, ADJACENT_SWAP, SYMMETRICAL_SWAP, WILDCARD)
            .map {
                FuzzySearchScenario(
                    setOf("RAPHAEL"),
                    "raphael",
                    0,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = false),
                    listOf(
                        TrieSearchResult(
                            "RAPHAEL",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "RAPHAEL",
                                matchedWord = "RAPHAEL",
                                numberOfMatches = 7,
                                numberOfErrors = 0,
                                numberOfCaseMismatches = 7,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = true,
                                matchedWholeWord = true
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `all matching strategies work as expected when caseInsensitive = false`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX, ADJACENT_SWAP, SYMMETRICAL_SWAP, WILDCARD)
            .map {
                FuzzySearchScenario(
                    setOf("RAPHAEL"),
                    "raphael",
                    0,
                    it,
                    MatchingOptions(caseInsensitive = false, diacriticInsensitive = false),
                    listOf()
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `all matching strategies work as expected when diacriticInsensitive = true`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX, ADJACENT_SWAP, SYMMETRICAL_SWAP, WILDCARD)
            .map {
                FuzzySearchScenario(
                    setOf("raphaël"),
                    "raphael",
                    0,
                    it,
                    MatchingOptions(caseInsensitive = false, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "raphaël",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "raphaël",
                                matchedWord = "raphaël",
                                numberOfMatches = 7,
                                numberOfErrors = 0,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 1,
                                prefixDistance = 0,
                                matchedWholeString = true,
                                matchedWholeWord = true,
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `all matching strategies work as expected when diacriticInsensitive = false`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX, ADJACENT_SWAP, SYMMETRICAL_SWAP, WILDCARD)
            .map {
                FuzzySearchScenario(
                    setOf("raphaël"),
                    "raphael",
                    0,
                    it,
                    MatchingOptions(caseInsensitive = false, diacriticInsensitive = false),
                    listOf()
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, and EXACT_PREFIX matches with or without a space`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX)
            .map { listOf(
                FuzzySearchScenario(
                    setOf("fullstack", "full stack", "backend"),
                    "fullstack",
                    1,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "fullstack",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "fullstack",
                                matchedWord = "fullstack",
                                numberOfMatches = 9,
                                numberOfErrors = 0,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = true,
                                matchedWholeWord = true,
                            )
                        ),
                        TrieSearchResult(
                            "full stack",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "full stack",
                                matchedWord = "full stack",
                                numberOfMatches = 9,
                                numberOfErrors = 1,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        )
                    )
                ),
                FuzzySearchScenario(
                    setOf("fullstack", "full stack", "backend"),
                    "full stack",
                    1,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "full stack",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "full stack",
                                matchedWord = "full stack",
                                numberOfMatches = 10,
                                numberOfErrors = 0,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = true,
                                matchedWholeWord = true,
                            )
                        ),
                        TrieSearchResult(
                            "fullstack",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "fullstack",
                                matchedWord = "fullstack",
                                numberOfMatches = 9,
                                numberOfErrors = 1,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        )
                    )
                )
            )
        }.flatten()
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX do not match an edge case`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX)
            .map {
                FuzzySearchScenario(
                    setOf("ionice"),
                    "indices",
                    2,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf()
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFIX match missing characters in the data`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX)
            .map {
                FuzzySearchScenario(
                    setOf("this is rafael"),
                    "raphael",
                    2,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "this is rafael",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "rafael",
                                matchedWord = "rafael",
                                numberOfMatches = 5,
                                numberOfErrors = 2,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFIX match missing characters in the search keyword`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX)
            .map {
                FuzzySearchScenario(
                    setOf("this is raphael"),
                    "rafael",
                    2,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "this is raphael",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "raphael",
                                matchedWord = "raphael",
                                numberOfMatches = 5,
                                numberOfErrors = 2,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFIX match an incomplete string`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX)
            .map {
                FuzzySearchScenario(
                    setOf("ma", "man", "manu", "many"),
                    "manual",
                    3,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "manu",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "manu",
                                matchedWord = "manu",
                                numberOfMatches = 4,
                                numberOfErrors = 2,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        ),
                        TrieSearchResult(
                            "man",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "man",
                                matchedWord = "man",
                                numberOfMatches = 3,
                                numberOfErrors = 3,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        ),
                        TrieSearchResult(
                            "many",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "man",
                                matchedWord = "many",
                                numberOfMatches = 3,
                                numberOfErrors = 3,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFI match strings that stem from shorter, incomplete string`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX)
            .map {
                FuzzySearchScenario(
                    setOf("m", "ma", "man", "manXuXal"),
                    "manual",
                    3,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "manXuXal",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "manXuXal",
                                matchedWord = "manXuXal",
                                numberOfMatches = 6,
                                numberOfErrors = 2,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        ),
                        TrieSearchResult(
                            "man",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "man",
                                matchedWord = "man",
                                numberOfMatches = 3,
                                numberOfErrors = 3,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX match a super long word`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX)
            .map {
                FuzzySearchScenario(
                    setOf("blah blah indistinguishable blah blah"),
                    "indic",
                    1,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "blah blah indistinguishable blah blah",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "indi",
                                matchedWord = "indistinguishable",
                                numberOfMatches = 4,
                                numberOfErrors = 1,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = false,
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX match after an initial failed attempt, returning only the best possible match`() {
        arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX, FUZZY_POSTFIX)
            .map {
                FuzzySearchScenario(
                    setOf("lalala0 lalala1 lalala2 lalala3"),
                    "lalala2",
                    2,
                    it,
                    MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                    listOf(
                        TrieSearchResult(
                            "lalala0 lalala1 lalala2 lalala3",
                            Unit,
                            TrieSearchResultStats(
                                matchedSubstring = "lalala2",
                                matchedWord = "lalala2",
                                numberOfMatches = 7,
                                numberOfErrors = 0,
                                numberOfCaseMismatches = 0,
                                numberOfDiacriticMismatches = 0,
                                prefixDistance = 0,
                                matchedWholeString = false,
                                matchedWholeWord = true,
                            )
                        )
                    )
                )
            }
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategies LIBERAL, FUZZY_PREFIX, EXACT_PREFIX match with error between matching characters`() {
        listOf(
            arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX)
                .map {
                    FuzzySearchScenario(
                        setOf("indexes", "indices"),
                        "indices",
                        2,
                        it,
                        MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                        listOf(
                            TrieSearchResult(
                                "indices",
                                Unit,
                                TrieSearchResultStats(
                                    matchedSubstring = "indices",
                                    matchedWord = "indices",
                                    numberOfMatches = 7,
                                    numberOfErrors = 0,
                                    numberOfCaseMismatches = 0,
                                    numberOfDiacriticMismatches = 0,
                                    prefixDistance = 0,
                                    matchedWholeString = true,
                                    matchedWholeWord = true,
                                )
                            ),
                            TrieSearchResult(
                                "indexes",
                                Unit,
                                TrieSearchResultStats(
                                    matchedSubstring = "indexes",
                                    matchedWord = "indexes",
                                    numberOfMatches = 5,
                                    numberOfErrors = 2,
                                    numberOfCaseMismatches = 0,
                                    numberOfDiacriticMismatches = 0,
                                    prefixDistance = 0,
                                    matchedWholeString = false,
                                    matchedWholeWord = false,
                                )
                            )
                        )
                    )
                },
            arrayOf(LIBERAL, FUZZY_PREFIX, EXACT_PREFIX)
                .map {
                    FuzzySearchScenario(
                        setOf("indexes", "indices"),
                        "indexes",
                        2,
                        it,
                        MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                        listOf(
                            TrieSearchResult(
                                "indexes",
                                Unit,
                                TrieSearchResultStats(
                                    matchedSubstring = "indexes",
                                    matchedWord = "indexes",
                                    numberOfMatches = 7,
                                    numberOfErrors = 0,
                                    numberOfCaseMismatches = 0,
                                    numberOfDiacriticMismatches = 0,
                                    prefixDistance = 0,
                                    matchedWholeString = true,
                                    matchedWholeWord = true,
                                )
                            ),
                            TrieSearchResult(
                                "indices",
                                Unit,
                                TrieSearchResultStats(
                                    matchedSubstring = "indices",
                                    matchedWord = "indices",
                                    numberOfMatches = 5,
                                    numberOfErrors = 2,
                                    numberOfCaseMismatches = 0,
                                    numberOfDiacriticMismatches = 0,
                                    prefixDistance = 0,
                                    matchedWholeString = false,
                                    matchedWholeWord = false,
                                )
                            )
                        )
                    )
                }
        ).flatten()
            .forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategy LIBERAL matches errors in beginning`() {
        listOf(
            FuzzySearchScenario(
                setOf("lala 000123456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                0,
                LIBERAL,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "lala 000123456789000 hehe",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "123456789",
                            matchedWord = "000123456789000",
                            numberOfMatches = 9,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 3,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("lala 000x23456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                1,
                LIBERAL,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "lala 000x23456789000 hehe",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring =  "23456789",
                            matchedWord = "000x23456789000",
                            numberOfMatches = 8,
                            numberOfErrors = 1,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 4,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("lala 000x23456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                2,
                LIBERAL,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "lala 000x23456789000 hehe",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "23456789",
                            matchedWord = "000x23456789000",
                            numberOfMatches = 8,
                            numberOfErrors = 1,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 4,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    ),
                    TrieSearchResult(
                        "lala 000xx3456789000 hehe",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "3456789",
                            matchedWord = "000xx3456789000",
                            numberOfMatches = 7,
                            numberOfErrors = 2,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 5,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            )
        ).forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategy EXACT_PREFIX only matches exact beginning of word`() {
        val scenario = FuzzySearchScenario(
            setOf("lalala index", "lalala indix", "lalala ondex"),
            "index",
            1,
            EXACT_PREFIX,
            MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
            listOf(
                TrieSearchResult(
                    "lalala index",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "index",
                        matchedWord = "index",
                        numberOfMatches = 5,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = true,
                    )
                ),
                TrieSearchResult(
                    "lalala indix",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "indix",
                        matchedWord = "indix",
                        numberOfMatches = 4,
                        numberOfErrors = 1,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                )
            )
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matching strategy FUZZY_PREFIX only matches beginning of word with error tolerance`() {
        listOf(
            FuzzySearchScenario(
                setOf("lalaindex", "index", "ondex", "oldex", "omtex", "lalala index", "lalala ondex", "lalala oldex", "lalala omtex"),
                "index",
                1,
                FUZZY_PREFIX,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "index",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "index",
                            matchedWord = "index",
                            numberOfMatches = 5,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = true,
                            matchedWholeWord = true,
                        )
                    ),
                    TrieSearchResult(
                        "lalala index",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "index",
                            matchedWord = "index",
                            numberOfMatches = 5,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = true,
                        )
                    ),
                    TrieSearchResult(
                        "ondex",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "ndex",
                            matchedWord = "ondex",
                            numberOfMatches = 4,
                            numberOfErrors = 1,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 1,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    ),
                    TrieSearchResult(
                        "lalala ondex",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "ndex",
                            matchedWord = "ondex",
                            numberOfMatches = 4,
                            numberOfErrors = 1,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 1,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("lalaindex", "index", "ondex", "oldex", "omtex", "lalala index", "lalala ondex", "lalala oldex", "lalala omtex"),
                "index",
                2,
                FUZZY_PREFIX,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "index",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "index",
                            matchedWord = "index",
                            numberOfMatches = 5,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = true,
                            matchedWholeWord = true,
                        )
                    ),
                    TrieSearchResult(
                        "lalala index",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "index",
                            matchedWord = "index",
                            numberOfMatches = 5,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = true,
                        )
                    ),
                    TrieSearchResult(
                        "ondex",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "ndex",
                            matchedWord = "ondex",
                            numberOfMatches = 4,
                            numberOfErrors = 1,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 1,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    ),
                    TrieSearchResult(
                        "lalala ondex",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "ndex",
                            matchedWord = "ondex",
                            numberOfMatches = 4,
                            numberOfErrors = 1,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 1,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    ),
                    TrieSearchResult(
                        "oldex",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "dex",
                            matchedWord = "oldex",
                            numberOfMatches = 3,
                            numberOfErrors = 2,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 2,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    ),
                    TrieSearchResult(
                        "lalala oldex",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "dex",
                            matchedWord = "oldex",
                            numberOfMatches = 3,
                            numberOfErrors = 2,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 2,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            )
        ).forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategy FUZZY_POSTFIX will only accept errors at the end`() {
        val scenario = FuzzySearchScenario(
            setOf("rafael", "raphael", "raphaello", "raffael", "raffael", "raffaell", "raffaella", "raffaello"),
            "raffaello",
            2,
            FUZZY_POSTFIX,
            MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
            listOf(
                TrieSearchResult(
                    "raffaello",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "raffaello",
                        matchedWord = "raffaello",
                        numberOfMatches = 9,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = true,
                        matchedWholeWord = true,
                    )
                ),
                TrieSearchResult(
                    "raffaell",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "raffaell",
                        matchedWord = "raffaell",
                        numberOfMatches = 8,
                        numberOfErrors = 1,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),
                TrieSearchResult(
                    "raffaella",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "raffaell",
                        matchedWord = "raffaella",
                        numberOfMatches = 8,
                        numberOfErrors = 1,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),
                TrieSearchResult(
                    "raffael",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "raffael",
                        matchedWord = "raffael",
                        numberOfMatches = 7,
                        numberOfErrors = 2,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                )
            )
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matching strategy ADJACENT_SWAP will only match adjacent letter swaps`() {
        listOf(
            FuzzySearchScenario(
                setOf("raphael", "rapheal", "rafhael"),
                "rahpael",
                2,
                ADJACENT_SWAP,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "raphael",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "raphael",
                            matchedWord = "raphael",
                            numberOfMatches = 5,
                            numberOfErrors = 2,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("raphael", "rapheal", "rafhael"),
                "rahpael",
                4,
                ADJACENT_SWAP,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "raphael",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "raphael",
                            matchedWord = "raphael",
                            numberOfMatches = 5,
                            numberOfErrors = 2,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    ),
                    TrieSearchResult(
                        "rapheal",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "rapheal",
                            matchedWord = "rapheal",
                            numberOfMatches = 3,
                            numberOfErrors = 4,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            )
        ).forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategy SYMMETRICAL_SWAP will find letter swaps anywhere in the string`() {
        listOf(
            FuzzySearchScenario(
                setOf("i need Belly Jeans now"), // a symmetrical spoonerism in the middle of the string
                "Jelly Beans",
                2,
                SYMMETRICAL_SWAP,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "i need Belly Jeans now",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "Belly Jeans",
                            matchedWord = "Belly Jeans",
                            numberOfMatches = 9,
                            numberOfErrors = 2,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("Nuenas Boches"), // another symmetrical spoonerism
                "Buenas Noches",
                2,
                SYMMETRICAL_SWAP,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "Nuenas Boches",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "Nuenas Boches",
                            matchedWord = "Nuenas Boches",
                            numberOfMatches = 11,
                            numberOfErrors = 2,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("Chied Frurros"), // a symmetrical spoonerism with two letters
                "Fried Churros",
                4,
                SYMMETRICAL_SWAP,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "Chied Frurros",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "Chied Frurros",
                            matchedWord = "Chied Frurros",
                            numberOfMatches = 9,
                            numberOfErrors = 4,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("Nood Gight"), // a symmetrical spoonerism that won't match
                "Good Fight",
                2,
                SYMMETRICAL_SWAP,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf()
            )
        ).forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategy WILDCARD will matching wildcard characters without error`() {
        listOf(
            FuzzySearchScenario(
                setOf("rafael", "raphael"),
                "ra*ael",
                0,
                WILDCARD,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "rafael",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "rafael",
                            matchedWord = "rafael",
                            numberOfMatches = 6,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = true,
                            matchedWholeWord = true,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("rafael", "raphael"),
                "ra*ael",
                1,
                WILDCARD,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "rafael",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "rafael",
                            matchedWord = "rafael",
                            numberOfMatches = 6,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = true,
                            matchedWholeWord = true,
                        )
                    ),
                    TrieSearchResult(
                        "raphael",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "raphael",
                            matchedWord = "raphael",
                            numberOfMatches = 6,
                            numberOfErrors = 1,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            )
        ).forEach { runTestScenario(it) }
    }

    @Test
    fun `matching strategy ACRONYM will match strings containing the provided acronym`() {
        listOf(
            FuzzySearchScenario(
                setOf("I want to work at National Aeronautics and Space Administration"),
                "NASA",
                1,
                ACRONYM,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "I want to work at National Aeronautics and Space Administration",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "National Aeronautics and Space Administration",
                            matchedWord = "National Aeronautics and Space Administration",
                            numberOfMatches = 4,
                            numberOfErrors = 1,  // one error for the 'and'
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("I DON'T want to work at National Security Agency"),
                "NASA",
                1,
                ACRONYM,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf(
                    TrieSearchResult(
                        "I DON'T want to work at National Security Agency",
                        Unit,
                        TrieSearchResultStats(
                            matchedSubstring = "National Security Agency",
                            matchedWord = "National Security Agency",
                            numberOfMatches = 3,
                            numberOfErrors = 1, // one error for the missing 'a'
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            prefixDistance = 0,
                            matchedWholeString = false,
                            matchedWholeWord = false,
                        )
                    )
                )
            ),
            FuzzySearchScenario(
                setOf("I DON'T want to work at National Security Agency"),
                "NASA",
                0,
                ACRONYM,
                MatchingOptions(caseInsensitive = true, diacriticInsensitive = true),
                listOf()
            )
        ).forEach { runTestScenario(it) }
    }
}
