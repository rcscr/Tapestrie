package com.rcs.trie

import org.assertj.core.api.SoftAssertions
import kotlin.test.Test

class TrieSubstringMatchTest {

    data class SubstringMatchScenario(
        val description: String,
        val entries: Set<String>,
        val search: String,
        val expectedResults: List<TrieSearchResult<Unit>>
    )

    @Test
    fun `test matchBySubstring with predefined scenarios`(): Unit = with(substringMatchScenarios())  {
        val softAssertions = SoftAssertions()

        this.forEach { scenario ->
            // Arrange
            val trie = Trie<Unit>()
            scenario.entries.forEach {
                trie.put(it, Unit)
            }

            // Act
            val result = trie.matchBySubstring(scenario.search)

            // Assert
            softAssertions.assertThat(result)
                .`as`(scenario.description)
                .isEqualTo(scenario.expectedResults)
        }

        softAssertions.assertAll()
    }

    private fun substringMatchScenarios(): List<SubstringMatchScenario> {
        return listOf(
            SubstringMatchScenario(
                "Matches a prefix of length 1",
                setOf("abcdef", "hijklm"),
                "a",
                listOf(TrieSearchResult("abcdef", Unit, "a", "abcdef", 1, 0, 0, false, false))
            ),
            SubstringMatchScenario(
                "Matches a prefix of length > 1",
                setOf("abcdef", "defghi"),
                "def",
                listOf(
                    TrieSearchResult("defghi", Unit, "def", "defghi", 3, 0, 0, false, false),
                    TrieSearchResult("abcdef", Unit, "def", "abcdef", 3, 0, 3, false, false)
                )
            ),
            SubstringMatchScenario(
                "Matches a postfix",
                setOf("defghi", "jklmno"),
                "ghi",
                listOf(TrieSearchResult("defghi", Unit, "ghi", "defghi", 3, 0, 3, false, false))
            ),
            SubstringMatchScenario(
                "Matches a string within",
                setOf("deghij", "jklmno"),
                "ghi",
                listOf(TrieSearchResult("deghij", Unit, "ghi", "deghij", 3, 0, 2, false, false))
            ),
            SubstringMatchScenario(
                "Matches the whole sequence",
                setOf("jklmno", "jklmnp"),
                "jklmno",
                listOf(TrieSearchResult("jklmno", Unit, "jklmno", "jklmno", 6, 0, 0, true, true))
            ),
            SubstringMatchScenario(
                "Matches after an initial failed attempt",
                setOf("pqrpqs"),
                "pqs",
                listOf(TrieSearchResult("pqrpqs", Unit, "pqs", "pqrpqs", 3, 0, 3, false, false))
            ),
            SubstringMatchScenario(
                "Matches whole word",
                setOf("tu vw, xyz"),
                "vw",
                listOf(TrieSearchResult("tu vw, xyz", Unit, "vw", "vw", 2, 0, 0, false, true))
            ),
            SubstringMatchScenario(
                "Does not match partial match",
                setOf("123"),
                "234",
                listOf()
            )
        )
    }
}