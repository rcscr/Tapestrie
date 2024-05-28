package com.rcs.trie

class TrieSearchResultComparator {

    companion object {
        private val sortByLengthOfMatchLongestFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::lengthOfMatch).reversed()

        private val sortByLengthOfStringShortestFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.string.length }

        private val sortByMatchedSequenceTrueFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::matchedWholeWord).reversed()

        private val sortByPrefixDistanceShortestFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::prefixDistance)

        private val sortByWordLengthShortestFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::wordLength)

        private val sortByMatchedWholeWordTrueFirst: Comparator<TrieSearchResult<*>> =
            compareBy(TrieSearchResult<*>::matchedWholeWord).reversed()

        private val sortByLessErrorsFirst: Comparator<TrieSearchResult<*>> =
            compareBy { it.errors }

        val sortByBestMatchFirst: Comparator<TrieSearchResult<*>> =
            sortByPrefixDistanceShortestFirst
                .thenComparing(sortByLengthOfMatchLongestFirst)
                .thenComparing(sortByWordLengthShortestFirst)
                .thenComparing(sortByMatchedSequenceTrueFirst)
                .thenComparing(sortByMatchedWholeWordTrueFirst)
                .thenComparing(sortByLengthOfStringShortestFirst)
                .thenComparing(sortByLessErrorsFirst)
    }
}