package com.rcs.trie

import kotlin.test.Test
import com.rcs.trie.FuzzySubstringMatchingStrategy.*
import org.assertj.core.api.Assertions.assertThat

class TrieFuzzySearchTest {

    data class FuzzySearchScenario(
        val entries: Set<String>,
        val search: String,
        val errorTolerance: Int,
        val matchingStrategy: FuzzySubstringMatchingStrategy,
        val expectedResults: List<TrieSearchResult<Unit>>
    )

    private fun runTestScenario(scenario: FuzzySearchScenario) {
        // Arrange
        val trie = Trie<Unit>()
        scenario.entries.forEach {
            trie.put(it, Unit)
        }

        // Act
        val result = trie.matchBySubstringFuzzy(
            scenario.search, scenario.errorTolerance, scenario.matchingStrategy)

        // Assert
        assertThat(result)
            .isEqualTo(scenario.expectedResults)
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
                    listOf(
                        TrieSearchResult("this is rafael", Unit, "rafael", "rafael", 5, 2, 0, false, false)
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
                    listOf(
                        TrieSearchResult("this is raphael", Unit, "raphael", "raphael", 5, 2, 0, false, false)
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
                    listOf(
                        TrieSearchResult("manu", Unit, "manu", "manu", 4, 2, 0, false, false),
                        TrieSearchResult("man", Unit, "man", "man", 3, 3, 0, false, false),
                        TrieSearchResult("many", Unit, "man", "many", 3, 3, 0, false, false),
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
                    listOf(
                        TrieSearchResult("manXuXal", Unit, "manXuXal", "manXuXal", 6, 2, 0, false, false),
                        TrieSearchResult("man", Unit, "man", "man", 3, 3, 0, false, false),
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
                    listOf(TrieSearchResult("blah blah indistinguishable blah blah", Unit, "indi", "indistinguishable", 4, 1, 0, false, false))
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
                    listOf(TrieSearchResult("lalala0 lalala1 lalala2 lalala3", Unit, "lalala2", "lalala2", 7, 0, 0, false, true))
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
                        listOf(
                            TrieSearchResult("indices", Unit, "indices", "indices", 7, 0, 0, true, true),
                            TrieSearchResult("indexes", Unit, "indexes", "indexes", 5, 2, 0, false, false),
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
                        listOf(
                            TrieSearchResult("indexes", Unit, "indexes", "indexes", 7, 0, 0, true, true),
                            TrieSearchResult("indices", Unit, "indices", "indices", 5, 2, 0, false, false)
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
                listOf(TrieSearchResult("lala 000123456789000 hehe", Unit, "123456789", "000123456789000", 9, 0, 3, false, false))
            ),
            FuzzySearchScenario(
                setOf("lala 000x23456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                1,
                LIBERAL,
                listOf(TrieSearchResult("lala 000x23456789000 hehe", Unit, "23456789", "000x23456789000", 8, 1, 4, false, false))
            ),
            FuzzySearchScenario(
                setOf("lala 000x23456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                2,
                LIBERAL,
                listOf(
                    TrieSearchResult("lala 000x23456789000 hehe", Unit, "23456789", "000x23456789000", 8, 1, 4, false, false),
                    TrieSearchResult("lala 000xx3456789000 hehe", Unit, "3456789", "000xx3456789000", 7, 2, 5, false, false)
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
            listOf(
                TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
                TrieSearchResult("lalala indix", Unit, "indix", "indix", 4, 1, 0, false, false)
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
                listOf(
                    TrieSearchResult("index", Unit, "index", "index", 5, 0, 0, true, true),
                    TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
                    TrieSearchResult("ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
                    TrieSearchResult("lalala ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
                )
            ),
            FuzzySearchScenario(
                setOf("lalaindex", "index", "ondex", "oldex", "omtex", "lalala index", "lalala ondex", "lalala oldex", "lalala omtex"),
                "index",
                2,
                FUZZY_PREFIX,
                listOf(
                    TrieSearchResult("index", Unit, "index", "index", 5, 0, 0, true, true),
                    TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
                    TrieSearchResult("ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
                    TrieSearchResult("lalala ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
                    TrieSearchResult("oldex", Unit, "dex", "oldex", 3, 2, 2, false, false),
                    TrieSearchResult("lalala oldex", Unit, "dex", "oldex", 3, 2, 2, false, false)
                )
            )
        ).forEach { runTestScenario(it) }
    }

    @Test
    fun `returns results sorted by best match`() {
        val scenario = FuzzySearchScenario(
            setOf("manual", "manuel", "manuem", "emanuel", "lemanuel", "lemanuell", "manually", "manuals", "linux manual"),
            "manual",
            3,
            LIBERAL,
            listOf(
                // matches whole sequence is highest ranking
                TrieSearchResult("manual", Unit, "manual", "manual", 6, 0, 0, true, true),
                // matches a whole word is second-highest ranking
                TrieSearchResult("linux manual", Unit, "manual", "manual", 6, 0, 0, false, true),
                // matches the highest possible number of characters, but it's neither the whole sequence nor a whole word
                TrieSearchResult("manuals", Unit, "manual", "manuals", 6, 0, 0, false, false),
                // same as above, but the string is longer, so is ranked lower
                TrieSearchResult("manually", Unit, "manual", "manually", 6, 0, 0, false, false),
                // partial match, with one error
                TrieSearchResult("manuel", Unit, "manuel", "manuel", 5, 1, 0, false, false),
                // partial match, with two errors
                TrieSearchResult("manuem", Unit, "manu", "manuem", 4, 2, 0, false, false),
                // prefix match = 1
                TrieSearchResult("emanuel", Unit, "manuel", "emanuel", 5, 1, 1, false, false),
                // prefix match = 2
                TrieSearchResult("lemanuel", Unit, "manuel", "lemanuel", 5, 1, 2, false, false),
                // prefix match = 2 but word is longer, so ranked lower
                TrieSearchResult("lemanuell", Unit, "manuel", "lemanuell", 5, 1, 2, false, false)
            )
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matching strategy FUZZY_POSTFIX will only accept errors at the end`() {
        val scenario = FuzzySearchScenario(
            setOf("rafael", "raphael", "raphaello", "raffael", "laraffael", "raffaell", "raffaella", "raffaello"),
            "raffaello",
            2,
            FUZZY_POSTFIX,
            listOf(
                TrieSearchResult("raffaello", Unit, "raffaello", "raffaello", 9, 0, 0, true, true),
                TrieSearchResult("raffaell", Unit, "raffaell", "raffaell", 8, 1, 0, false, false),
                TrieSearchResult("raffaella", Unit, "raffaell", "raffaella", 8, 1, 0, false, false),
                TrieSearchResult("raffael", Unit, "raffael", "raffael", 7, 2, 0, false, false),
                TrieSearchResult("laraffael", Unit, "raffael", "laraffael", 7, 2, 2, false, false)
            )
        )
        runTestScenario(scenario)
    }
}
