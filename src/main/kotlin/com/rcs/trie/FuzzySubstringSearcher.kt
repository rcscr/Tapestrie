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
            queue.add(initialState(root, search, errorTolerance))

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()

                if (state.sufficientlyMatches()) {
                    val newMatches = gatherAll(state, matchingStrategy)
                    results.putOnlyNewOrBetterMatches(newMatches)
                    continue
                }

                synchronized(state.node.next) {
                    for (nextNode in state.node.next) {
                        queue.addAll(state.nextSearchStates(nextNode, matchingStrategy))
                    }
                }
            }

            return results.values.sortedWith(TrieSearchResultComparator.byBestMatchFirst)
        }

        private fun <T> gatherAll(
            initialState: FuzzySubstringSearchState<T>,
            matchingStrategy: FuzzySubstringMatchingStrategy
        ): MutableMap<String, TrieSearchResult<T>> {

            val results = mutableMapOf<String, TrieSearchResult<T>>()
            val queue = ArrayDeque<FuzzySubstringSearchState<T>>()
            queue.add(initialState)

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()

                if (state.node.completes()) {
                    val searchResult = state.buildSearchResult(matchingStrategy)
                    results[searchResult.string] = searchResult
                }

                synchronized(state.node.next) {
                    for (nextNode in state.node.next) {
                        queue.add(state.nextBuildState(nextNode))
                    }
                }
            }

            return results
        }

        private fun <T> initialState(
            root: TrieNode<T>,
            search: String,
            errorTolerance: Int
        ): FuzzySubstringSearchState<T> {

            return FuzzySubstringSearchState(
                search = search,
                node = root,
                startMatchIndex = null,
                endMatchIndex =  null,
                searchIndex = 0,
                numberOfMatches = 0,
                numberOfErrors = 0,
                errorTolerance,
                sequence = StringBuilder()
            )
        }

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