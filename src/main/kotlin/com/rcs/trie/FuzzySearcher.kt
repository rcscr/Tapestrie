package com.rcs.trie

import java.util.concurrent.Executors

class FuzzySearcher {

    companion object {

        private val updateLock = Any()

        fun <T> search(
            root: TrieNode<T>,
            search: String,
            errorTolerance: Int,
            matchingStrategy: FuzzyMatchingStrategy
        ): List<TrieSearchResult<T>> {

            if (search.isEmpty() || errorTolerance < 0 || errorTolerance > search.length) {
                throw IllegalArgumentException()
            }

            val executorService = Executors.newVirtualThreadPerTaskExecutor()

            val initialStates = FuzzySearchState.getInitialStates(root, search, errorTolerance, matchingStrategy)
            val results = mutableMapOf<String, TrieSearchResult<T>>()

            // Parallelizes only top-level of the Trie:
            // one (virtual) thread for each state derived from each node directly beneath the root
            val topLevelStates = initialStates
                .map { it.nextStates() }
                .flatten()

            val futures = topLevelStates.map {
                executorService.submit {
                    searchJob(it, results)
                }
            }

            futures.map { it.get() }

            // clean up resources to prevent memory leak
            executorService.submit {
                System.gc()
                executorService.shutdown()
            }

            return results.values.sortedWith(TrieSearchResultComparator.byBestMatchFirst)
        }

        private fun <T> searchJob(
            initialState: FuzzySearchState<T>,
            results: MutableMap<String, TrieSearchResult<T>>
        ) {
            val queue = ArrayDeque<FuzzySearchState<T>>()
            queue.add(initialState)

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()

                if (state.hasSearchResult()) {
                    val searchResult = state.buildSearchResult()
                    results.putOnlyNewOrBetter(searchResult)
                }

                queue.addAll(state.nextStates())
            }
        }

        private fun <T> MutableMap<String, TrieSearchResult<T>>.putOnlyNewOrBetter(newMatch: TrieSearchResult<T>) {
            synchronized(updateLock) {
                this[newMatch.string] = when (val existing = this[newMatch.string]) {
                    null -> newMatch
                    else -> {
                        val compareResult = TrieSearchResultComparator.byBestMatchFirst.compare(newMatch, existing)
                        val newMatchIsBetter = compareResult == -1
                        when {
                            newMatchIsBetter -> newMatch
                            else -> existing
                        }
                    }
                }
            }
        }
    }
}