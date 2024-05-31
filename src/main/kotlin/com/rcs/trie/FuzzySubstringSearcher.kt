package com.rcs.trie

class FuzzySubstringSearcher {

    companion object {

        fun <T> search(
            root: TrieNode<T>,
            search: String,
            errorTolerance: Int,
            matchingStrategy: FuzzySubstringMatchingStrategy
        ): List<TrieSearchResult<T>> {

            if (search.isEmpty() || errorTolerance < 0 || errorTolerance > search.length) {
                throw IllegalArgumentException()
            }

            val results = mutableMapOf<String, TrieSearchResult<T>>()

            val queue = ArrayDeque<FuzzySubstringSearchState<T>>()
            queue.addAll(getInitialStates(root, search, errorTolerance, matchingStrategy))

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()

                if (state.sufficientlyMatches()) {
                    val newMatches = gatherAll(state)
                    when(matchingStrategy) {
                        FuzzySubstringMatchingStrategy.LIBERAL ->
                            results.putOnlyNewOrBetterMatches(newMatches)
                        else ->
                            results.putAll(newMatches)
                    }
                    continue
                }

                queue.addAll(state.nextSearchStates())
            }

            return results.values.sortedWith(TrieSearchResultComparator.byBestMatchFirst)
        }

        private fun <T> gatherAll(
            initialState: FuzzySubstringSearchState<T>
        ): MutableMap<String, TrieSearchResult<T>> {

            val results = mutableMapOf<String, TrieSearchResult<T>>()
            val queue = ArrayDeque<FuzzySubstringSearchState<T>>()
            queue.add(initialState)

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()

                if (state.completes()) {
                    val searchResult = state.buildSearchResult()
                    results[searchResult.string] = searchResult
                }

                queue.addAll(state.nextBuildStates())
            }

            return results
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
                        errorTolerance,
                        matchingStrategy
                    )
                    initialStates.add(stateWithPredeterminedError)
                }
            }

            return initialStates
        }

        /**
         * Only needed for FuzzySubstringMatchingStrategy.LIBERAL
         */
        private fun <T> MutableMap<String, TrieSearchResult<T>>
                .putOnlyNewOrBetterMatches(newMatches: MutableMap<String, TrieSearchResult<T>>) {
            newMatches.entries
                .filter {
                    this[it.key] == null
                            || this[it.key]!!.numberOfMatches < it.value.numberOfMatches
                            || this[it.key]!!.numberOfErrors > it.value.numberOfErrors
                }
                .forEach { this[it.key] = it.value }
        }
    }
}