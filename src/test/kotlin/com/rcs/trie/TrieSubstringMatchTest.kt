package com.rcs.trie

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class TrieSubstringMatchTest {

    data class SubstringMatchScenario(
        val entries: Set<String>,
        val search: String,
        val expectedResults: List<TrieSearchResult<Unit>>
    )

    private fun runTestScenario(scenario: SubstringMatchScenario) {
        // Arrange
        val trie = Trie<Unit>()
        scenario.entries.forEach {
            trie.put(it, Unit)
        }

        // Act
        val result = trie.matchBySubstring(scenario.search)

        // Assert
        assertThat(result)
            .isEqualTo(scenario.expectedResults)
    }

    @Test
    fun `matches a prefix of length 1`() {
        val scenario = SubstringMatchScenario(
            setOf("abcdef", "hijklm"),
            "a",
            listOf(
                TrieSearchResult(
                    "abcdef",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "a",
                        matchedWord = "abcdef",
                        numberOfMatches = 1,
                        numberOfErrors = 0,
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
    fun `matches a prefix of length greater than 1`() {
        val scenario = SubstringMatchScenario(
            setOf("abcdef", "defghi"),
            "def",
            listOf(
                TrieSearchResult(
                    "defghi",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "def",
                        matchedWord = "defghi",
                        numberOfMatches = 3,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),
                TrieSearchResult(
                    "abcdef",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "def",
                        matchedWord = "abcdef",
                        numberOfMatches = 3,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 3,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                )
            )
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matches a postfix`() {
        val scenario = SubstringMatchScenario(
            setOf("defghi", "jklmno"),
            "ghi",
            listOf(
                TrieSearchResult(
                    "defghi",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "ghi",
                        matchedWord = "defghi",
                        numberOfMatches = 3,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 3,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                )
            )
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matches a string within`() {
        val scenario = SubstringMatchScenario(
            setOf("deghij", "jklmno"),
            "ghi",
            listOf(
                TrieSearchResult(
                    "deghij",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "ghi",
                        matchedWord = "deghij",
                        numberOfMatches = 3,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 2,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                )
            )
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matches the whole sequence`() {
        val scenario = SubstringMatchScenario(
            setOf("jklmno", "jklmnp"),
            "jklmno",
            listOf(
                TrieSearchResult(
                    "jklmno",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "jklmno",
                        matchedWord = "jklmno",
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
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matches after an initial failed attempt`() {
        val scenario = SubstringMatchScenario(
            setOf("pqrpqs"),
            "pqs",
            listOf(
                TrieSearchResult(
                    "pqrpqs",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "pqs",
                        matchedWord = "pqrpqs",
                        numberOfMatches = 3,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 3,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                )
            )
        )
        runTestScenario(scenario)
    }

    @Test
    fun `matches whole word`() {
        val scenario = SubstringMatchScenario(
            setOf("tu vw, xyz"),
            "vw",
            listOf(
                TrieSearchResult(
                    "tu vw, xyz",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "vw",
                        matchedWord = "vw",
                        numberOfMatches = 2,
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
        runTestScenario(scenario)
    }

    @Test
    fun `does not match partial match`() {
        val scenario = SubstringMatchScenario(
            setOf("123"),
            "234",
            listOf()
        )
        runTestScenario(scenario)
    }
}