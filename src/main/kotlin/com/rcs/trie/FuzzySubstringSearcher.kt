package com.rcs.trie

import java.util.concurrent.Executors

class FuzzySubstringSearcher {

    companion object {

        private val updateLock = Any()

        fun <T> search(
            root: TrieNode<T>,
            search: String,
            errorTolerance: Int,
            matchingStrategy: FuzzySubstringMatchingStrategy
        ): List<TrieSearchResult<T>> {

            if (search.isEmpty() || errorTolerance < 0 || errorTolerance > search.length) {
                throw IllegalArgumentException()
            }

            val executorService = Executors.newVirtualThreadPerTaskExecutor()

            val initialStates = getInitialStates(root, search, errorTolerance, matchingStrategy)
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
            initialState: FuzzySubstringSearchState<T>,
            results: MutableMap<String, TrieSearchResult<T>>
        ) {
            val queue = ArrayDeque<FuzzySubstringSearchState<T>>()
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

        private fun <T> getInitialStates(
            root: TrieNode<T>,
            search: String,
            errorTolerance: Int,
            matchingStrategy: FuzzySubstringMatchingStrategy
        ): Collection<FuzzySubstringSearchState<T>> {

            val initialStates = mutableListOf<FuzzySubstringSearchState<T>>()

            val defaultInitialState = FuzzySubstringSearchState(
                root, search, 0, errorTolerance, matchingStrategy)

            initialStates.add(defaultInitialState)

            // efficient way to match with errors in beginning
            if (matchingStrategy == FuzzySubstringMatchingStrategy.LIBERAL) {
                for (i in 1..errorTolerance) {
                    val stateWithPredeterminedError = FuzzySubstringSearchState(
                        root,
                        search.substring(i, search.length),
                        numberOfPredeterminedErrors = i,
                        errorTolerance - i,
                        matchingStrategy
                    )
                    initialStates.add(stateWithPredeterminedError)
                }
            }

            return initialStates
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