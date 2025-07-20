package com.rcs.trie

class TrieSearchResultComparator {

    companion object {

        private val byNumberOfMatchesMoreFirst: Comparator<TrieSearchResult<*>> =
            compareBy<TrieSearchResult<*>> { it.stats.numberOfMatches }.reversed()

        private val byLengthOfStringShortestFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.string.length }

        private val byMatchedSequenceTrueFirst: Comparator<TrieSearchResult<*>> =
            compareBy<TrieSearchResult<*>> { it.stats.matchedWholeWord }.reversed()

        private val byPrefixDistanceShortestFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.stats.prefixDistance }

        private val byWordLengthShortestFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.stats.matchedWord.length }

        private val byMatchedWholeWordTrueFirst: Comparator<TrieSearchResult<*>> =
            compareBy<TrieSearchResult<*>> { it.stats.matchedWholeWord }.reversed()

        private val byNumberOfErrorsLessFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.stats.numberOfErrors }

        private val byNumberOfCaseMismatchesLessFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.stats.numberOfCaseMismatches }

        private val byNumberOfDiacriticMismatchesLessFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.stats.numberOfDiacriticMismatches }

        val byBestMatchFirst: Comparator<TrieSearchResult<*>> =
            byPrefixDistanceShortestFirst
                .thenComparing(byNumberOfMatchesMoreFirst)
                .thenComparing(byWordLengthShortestFirst)
                .thenComparing(byMatchedSequenceTrueFirst)
                .thenComparing(byMatchedWholeWordTrueFirst)
                .thenComparing(byLengthOfStringShortestFirst)
                .thenComparing(byNumberOfErrorsLessFirst)
                .thenComparing(byNumberOfCaseMismatchesLessFirst)
                .thenComparing(byNumberOfDiacriticMismatchesLessFirst)
    }
}