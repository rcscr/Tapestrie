package com.rcs.trie.utils

import com.rcs.trie.FuzzyMatchingStrategy
import com.rcs.trie.MatchingOptions
import com.rcs.trie.Trie
import com.rcs.trie.TrieSearchResult
import org.assertj.core.api.Assertions.assertThat

class TestUtils {

    data class FuzzySearchScenario(
        val entries: Set<String>,
        val search: String,
        val errorTolerance: Int,
        val matchingStrategy: FuzzyMatchingStrategy,
        val matchingOptions: MatchingOptions,
        val expectedResults: List<TrieSearchResult<Unit>>
    )

    companion object {

        fun runTestScenario(scenario: FuzzySearchScenario) {
            // Arrange
            val trie = Trie<Unit>()
            scenario.entries.forEach {
                trie.put(it, Unit)
            }

            // Act
            val result = trie.matchBySubstringFuzzy(
                scenario.search, scenario.errorTolerance, scenario.matchingStrategy, scenario.matchingOptions
            )

            // Assert
            assertThat(result)
                .isEqualTo(scenario.expectedResults)
        }
    }
}