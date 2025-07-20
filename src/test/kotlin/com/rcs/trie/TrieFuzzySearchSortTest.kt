package com.rcs.trie

import com.rcs.trie.FuzzyMatchingStrategy.LIBERAL
import com.rcs.trie.utils.TestUtils.FuzzySearchScenario
import com.rcs.trie.utils.TestUtils.Companion.runTestScenario
import kotlin.test.Test

class TrieFuzzySearchSortTest {

    @Test
    fun `returns results sorted by best match`() {
        val scenario = FuzzySearchScenario(
            setOf(
                "manual",
                "manuel",
                "manuem",
                "emanuel",
                "lemanuel",
                "lemanuell",
                "manually",
                "manuals",
                "linux manual"
            ),
            "manual",
            3,
            LIBERAL,
            MatchingOptions.allDisabled, // todo: test sort based on these options
            listOf(
                // matches whole sequence is highest ranking
                TrieSearchResult(
                    "manual",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manual",
                        matchedWord = "manual",
                        numberOfMatches = 6,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = true,
                        matchedWholeWord = true,
                    )
                ),

                // matches a whole word is second-highest ranking
                TrieSearchResult(
                    "linux manual",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manual",
                        matchedWord = "manual",
                        numberOfMatches = 6,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = true,
                    )
                ),

                // matches the highest possible number of characters, but it's neither the whole sequence nor a whole word
                TrieSearchResult(
                    "manuals",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manual",
                        matchedWord = "manuals",
                        numberOfMatches = 6,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),

                // same as above, but the string is longer, so is ranked lower
                TrieSearchResult(
                    "manually",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manual",
                        matchedWord = "manually",
                        numberOfMatches = 6,
                        numberOfErrors = 0,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),

                // partial match, with one error
                TrieSearchResult(
                    "manuel",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manuel",
                        matchedWord = "manuel",
                        numberOfMatches = 5,
                        numberOfErrors = 1,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),

                // partial match, with two errors
                TrieSearchResult(
                    "manuem",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manu",
                        matchedWord = "manuem",
                        numberOfMatches = 4,
                        numberOfErrors = 2,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 0,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),

                // prefix distance = 1
                TrieSearchResult(
                    "emanuel",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manuel",
                        matchedWord = "emanuel",
                        numberOfMatches = 5,
                        numberOfErrors = 1,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 1,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),

                // prefix distance = 2
                TrieSearchResult(
                    "lemanuel",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manuel",
                        matchedWord = "lemanuel",
                        numberOfMatches = 5,
                        numberOfErrors = 1,
                        numberOfCaseMismatches = 0,
                        numberOfDiacriticMismatches = 0,
                        prefixDistance = 2,
                        matchedWholeString = false,
                        matchedWholeWord = false,
                    )
                ),

                // prefix match = 2 but word is longer, so ranked lower
                TrieSearchResult(
                    "lemanuell",
                    Unit,
                    TrieSearchResultStats(
                        matchedSubstring = "manuel",
                        matchedWord = "lemanuell",
                        numberOfMatches = 5,
                        numberOfErrors = 1,
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
}