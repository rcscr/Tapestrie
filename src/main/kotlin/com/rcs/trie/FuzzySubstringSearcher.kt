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
            val initialState = FuzzySubstringSearchState(search, root, null, null, 0, 0, 0, errorTolerance, StringBuilder())
            queue.add(initialState)

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()

                if (state.sufficientlyMatches()) {
                    val newMatches = gatherAll(state)
                    results.putOnlyNewOrBetterMatches(newMatches)
                    continue
                }

                var nextNodes: Array<TrieNode<T>>
                synchronized(state.node.next) {
                    nextNodes = state.node.next.toTypedArray()
                }
                for (nextNode in nextNodes) {
                    queue.addAll(state.nextSearchStates(nextNode, matchingStrategy))
                }
            }

            return results.values.sortedWith(TrieSearchResultComparator.sortByBestMatchFirst)
        }

        private fun <T> gatherAll(initialState: FuzzySubstringSearchState<T>): MutableMap<String, TrieSearchResult<T>> {
            val results = mutableMapOf<String, TrieSearchResult<T>>()
            val queue = ArrayDeque<FuzzySubstringSearchState<T>>()
            queue.add(initialState)

            while (queue.isNotEmpty()) {
                val state = queue.removeFirst()

                if (state.node.completes()) {
                    val searchResult = state.buildSearchResult()
                    results[searchResult.string] = searchResult
                }

                var nextNodes: Array<TrieNode<T>>
                synchronized(state.node.next) {
                    nextNodes = state.node.next.toTypedArray()
                }
                for (nextNode in nextNodes) {
                    queue.add(state.nextBuildState(nextNode))
                }
            }

            return results
        }

        private fun <T> MutableMap<String, TrieSearchResult<T>>
                .putOnlyNewOrBetterMatches(newMatches: MutableMap<String, TrieSearchResult<T>>) {
            newMatches.entries
                .filter {
                    this[it.key] == null
                            || this[it.key]!!.lengthOfMatch < it.value.lengthOfMatch
                            || this[it.key]!!.errors > it.value.errors
                }
                .forEach { this[it.key] = it.value }
        }
    }
}