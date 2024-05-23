package com.rcs.trie

import com.rcs.trie.Trie.SearchResult

class TrieResultComparator {

    companion object {
        private val sortByLengthOfMatchLongestFirst: Comparator<SearchResult<*>> =
            compareBy(SearchResult<*>::lengthOfMatch).reversed()

        private val sortByLengthOfStringShortestFirst: Comparator<SearchResult<*>> =
            compareBy { it.string.length }

        private val sortByMatchedSequenceTrueFirst: Comparator<SearchResult<*>> =
            compareBy(SearchResult<*>::matchedWholeWord).reversed()

        private val sortByMatchedWholeWordTrueFirst: Comparator<SearchResult<*>> =
            compareBy(SearchResult<*>::matchedWholeWord).reversed()

        private val sortByLessErrorsFirst: Comparator<SearchResult<*>> =
            compareBy { it.errors }

        val sortByBestMatchFirst: Comparator<SearchResult<*>> =
            sortByLengthOfMatchLongestFirst
                .thenComparing(sortByMatchedSequenceTrueFirst)
                .thenComparing(sortByMatchedWholeWordTrueFirst)
                .thenComparing(sortByLengthOfStringShortestFirst)
                .thenComparing(sortByLessErrorsFirst)
    }
}