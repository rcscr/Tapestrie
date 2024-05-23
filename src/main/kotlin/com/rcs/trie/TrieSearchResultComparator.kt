package com.rcs.trie

class TrieSearchResultComparator {

    companion object {
        private val sortByLengthOfMatchLongestFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::lengthOfMatch).reversed()

        private val sortByLengthOfStringShortestFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.string.length }

        private val sortByMatchedSequenceTrueFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::matchedWholeWord).reversed()

        private val sortByMatchedWholeWordTrueFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::matchedWholeWord).reversed()

        private val sortByLessErrorsFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.errors }

        val sortByBestMatchFirst: Comparator<TrieSearchResult<*>> =
            sortByLengthOfMatchLongestFirst
                .thenComparing(sortByMatchedSequenceTrueFirst)
                .thenComparing(sortByMatchedWholeWordTrueFirst)
                .thenComparing(sortByLengthOfStringShortestFirst)
                .thenComparing(sortByLessErrorsFirst)
    }
}