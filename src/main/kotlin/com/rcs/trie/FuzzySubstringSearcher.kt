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

                if (state.hasSearchResult()) {
                    // TODO: figure out why I'm getting duplicate search results
                    val searchResult = state.buildSearchResult()
                    results.putOnlyNewOrBetter(searchResult)
                }

                queue.addAll(state.nextStates())
            }

            return results.values.sortedWith(TrieSearchResultComparator.byBestMatchFirst)
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

        private fun <T> MutableMap<String, TrieSearchResult<T>>.putOnlyNewOrBetter(match: TrieSearchResult<T>) {
            val existing = this[match.string]

            if (existing == null) {
                this[match.string] = match
            } else {
                val bestFirst = mutableListOf(existing, match)
                    .sortedWith(TrieSearchResultComparator.byBestMatchFirst)
                this[match.string] = bestFirst.first()
            }
        }
    }
}