package com.rcs.trie

import org.assertj.core.api.SoftAssertions
import kotlin.test.Test

class TrieFuzzySearchTest {

    data class FuzzySearchScenario(
        val description: String,
        val entries: Set<String>,
        val search: String,
        val errorTolerance: Int,
        val matchingStrategy: FuzzySubstringMatchingStrategy,
        val expectedResults: List<TrieSearchResult<Unit>>
    )

    @Test
    fun `test matchBySubstringFuzzy with predefined scenarios`(): Unit = with(fuzzySearchScenarios())  {
        val softAssertions = SoftAssertions()

        this.forEach { scenario ->
            // Arrange
            val trie = Trie<Unit>()
            scenario.entries.forEach {
                trie.put(it, Unit)
            }

            // Act
            val result = trie.matchBySubstringFuzzy(
                scenario.search, scenario.errorTolerance, scenario.matchingStrategy)

            // Assert
            softAssertions.assertThat(result)
                .`as`(scenario.description)
                .isEqualTo(scenario.expectedResults)
        }

        softAssertions.assertAll()
    }

    private fun fuzzySearchScenarios(): List<FuzzySearchScenario> {
        val scenarios = mutableListOf<FuzzySearchScenario>()

        scenarios.addAll(
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it does not match an edge case",
                        setOf("ionice"),
                        "indices",
                        2,
                        it,
                        listOf()
                    )
                }
        )

        scenarios.addAll(
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it matches missing characters in the data",
                        setOf("this is rafael"),
                        "raphael",
                        2,
                        it,
                        listOf(
                            TrieSearchResult("this is rafael", Unit, "rafael", "rafael", 5, 2, 0, false, false)
                        )
                    )
                }
        )

        scenarios.addAll(
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it matches missing characters in the search keyword",
                        setOf("this is raphael"),
                        "rafael",
                        2,
                        it,
                        listOf(
                            TrieSearchResult("this is raphael", Unit, "raphael", "raphael", 5, 2, 0, false, false)
                        )
                    )
                }
        )

        scenarios.addAll(
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it matches an incomplete string, but only if it has enough characters to satisfy match",
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
        )

        scenarios.addAll(
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it matches a super long word",
                        setOf("blah blah indistinguishable blah blah"),
                        "indic",
                        1,
                        it,
                        listOf(TrieSearchResult("blah blah indistinguishable blah blah", Unit, "indi", "indistinguishable", 4, 1, 0, false, false))
                    )
                }
        )

        scenarios.addAll(
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it matches after an initial failed attempt, returning only the best possible match",
                        setOf("lalala0 lalala1 lalala2 lalala3"),
                        "lalala2",
                        2,
                        it,
                        listOf(TrieSearchResult("lalala0 lalala1 lalala2 lalala3", Unit, "lalala2", "lalala2", 7, 0, 0, false, true))
                    )
                }
        )

        scenarios.addAll(listOf(
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it matches with error between matching characters",
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
            FuzzySubstringMatchingStrategy.entries
                .map {
                    FuzzySearchScenario(
                        "MatchingStrategy=$it matches with error between matching characters",
                        setOf("indexes", "indices"),
                        "indexes",
                        2,
                        it,
                        listOf(
                            TrieSearchResult("indexes", Unit, "indexes", "indexes", 7, 0, 0, true, true),
                            TrieSearchResult("indices", Unit, "indices", "indices", 5, 2, 0, false, false)
                        )
                    )
                },
            ).flatten()
        )

        scenarios.addAll(listOf(
            FuzzySearchScenario(
                "MatchingStrategy=LIBERAL matches substring with errorTolerance=0",
                setOf("lala 000123456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                0,
                FuzzySubstringMatchingStrategy.LIBERAL,
                listOf(TrieSearchResult("lala 000123456789000 hehe", Unit, "123456789", "000123456789000", 9, 0, 3, false, false))
            ),
            FuzzySearchScenario(
                "MatchingStrategy=LIBERAL matches errors in beginning with errorTolerance=1",
                setOf("lala 000x23456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                1,
                FuzzySubstringMatchingStrategy.LIBERAL,
                listOf(TrieSearchResult("lala 000x23456789000 hehe", Unit, "23456789", "000x23456789000", 8, 1, 4, false, false))
            ),
            FuzzySearchScenario(
                "MatchingStrategy=LIBERAL matches errors in beginning with errorTolerance=2",
                setOf("lala 000x23456789000 hehe", "lala 000x23456789000 hehe", "lala 000xx3456789000 hehe", "lala 000xxx456789000 hehe"),
                "123456789",
                2,
                FuzzySubstringMatchingStrategy.LIBERAL,
                listOf(
                    TrieSearchResult("lala 000x23456789000 hehe", Unit, "23456789", "000x23456789000", 8, 1, 4, false, false),
                    TrieSearchResult("lala 000xx3456789000 hehe", Unit, "3456789", "000xx3456789000", 7, 2, 5, false, false)
                )
            )
        ))

        scenarios.addAll(listOf(
            FuzzySearchScenario(
                "MatchingStrategy=MATCH_PREFIX only matches exact beginning of word",
                setOf("lalala index", "lalala indix", "lalala ondex"),
                "index",
                1,
                FuzzySubstringMatchingStrategy.MATCH_PREFIX,
                listOf(
                    TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
                    TrieSearchResult("lalala indix", Unit, "indix", "indix", 4, 1, 0, false, false)
                )
            )
        ))

        scenarios.addAll(listOf(
            FuzzySearchScenario(
                "MatchingStrategy=ANCHOR_TO_PREFIX only matches beginning of word with errorTolerance=1",
                setOf("lalaindex", "index", "ondex", "oldex", "omtex", "lalala index", "lalala ondex", "lalala oldex", "lalala omtex"),
                "index",
                1,
                FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX,
                listOf(
                    TrieSearchResult("index", Unit, "index", "index", 5, 0, 0, true, true),
                    TrieSearchResult("lalala index", Unit, "index", "index", 5, 0, 0, false, true),
                    TrieSearchResult("ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
                    TrieSearchResult("lalala ondex", Unit, "ndex", "ondex", 4, 1, 1, false, false),
                )
            ),
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
        ))

        scenarios.add(
            FuzzySearchScenario(
                "MatchingStrategy=LIBERAL will return results sorted by best match",
                setOf("manual", "manuel", "manuem", "emanuel", "lemanuel", "lemanuell", "manually", "manuals", "linux manual"),
                "manual",
                3,
                FuzzySubstringMatchingStrategy.LIBERAL,
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
        )

        return scenarios
    }
}
