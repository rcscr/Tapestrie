package com.rcs.trie

import kotlin.math.min

class FuzzySubstringSearchState<T> private constructor(
    private val searchRequest: SearchRequest,
    private val searchVariables: SearchVariables<T>,
    private val searchCoordinates: SearchCoordinates
) {

    /**
     * Invariable properties of the search request - these never change.
     */
    private data class SearchRequest(
        val matchingStrategy: FuzzySubstringMatchingStrategy,
        val search: String,
        val numberOfPredeterminedErrors: Int,
        val errorTolerance: Int,
    )

    /**
     * Variable properties that change depending on what TrieNode we're looking at.
     */
    private data class SearchVariables<T>(
        val node: TrieNode<T>,
        val sequence: StringBuilder,
        val isFinisherState: Boolean,
    )

    /**
     * The search coordinates, which may or may not lead to a successful match.
     */
    private data class SearchCoordinates(
        val searchIndex: Int,
        val numberOfMatches: Int,
        val numberOfErrors: Int,
        val startMatchIndex: Int?,
        var endMatchIndex: Int?,
    )

    /**
     * A convenience class for passing around new search error states.
     */
    private data class SearchWithErrorStrategy<T>(
        val node: TrieNode<T>,
        val searchIndex: Int,
        val sequence: StringBuilder
    )

    private val wordSeparatorRegex = "[\\s\\p{P}]".toRegex()

    fun nextStates(): Collection<FuzzySubstringSearchState<T>> {
        synchronized(searchVariables.node.next) {
            return searchVariables.node.next.map { nextStates(it) }.flatten()
        }
    }

    fun hasSearchResult(): Boolean {
        return searchVariables.node.completes() && sufficientlyMatches()
    }

    fun buildSearchResult(): TrieSearchResult<T> {
        assert(hasSearchResult())

        val actualErrors = getActualNumberOfErrors()

        val assertedStartMatchIndex = searchCoordinates.startMatchIndex!!

        val assertedEndMatchIndex = searchCoordinates.endMatchIndex!!

        val matchedWholeSequence = actualErrors == 0
                && matchedWholeSequence(assertedStartMatchIndex, assertedEndMatchIndex)

        val matchedWholeWord = actualErrors == 0
                && matchedWholeWord(assertedStartMatchIndex, assertedEndMatchIndex)

        val indexOfWordSeparatorBefore = searchVariables.sequence
            .indexOfLastWordSeparator(assertedStartMatchIndex) ?: -1

        val indexOfWordSeparatorAfter = searchVariables.sequence
            .indexOfFirstWordSeparator(assertedStartMatchIndex) ?: searchVariables.sequence.length

        val prefixDistance = assertedStartMatchIndex - indexOfWordSeparatorBefore - 1

        val matchedSubstring = searchVariables.sequence.substring(assertedStartMatchIndex, assertedEndMatchIndex + 1)

        val matchedWord = searchVariables.sequence.substring(indexOfWordSeparatorBefore + 1, indexOfWordSeparatorAfter)

        return TrieSearchResult(
            searchVariables.sequence.toString(),
            searchVariables.node.value!!,
            matchedSubstring,
            matchedWord,
            searchCoordinates.numberOfMatches,
            actualErrors,
            prefixDistance,
            matchedWholeSequence,
            matchedWholeWord
        )
    }

    private fun sufficientlyMatches(): Boolean {
        return searchCoordinates.startMatchIndex != null
                && searchCoordinates.endMatchIndex != null
                && searchCoordinates.numberOfMatches >= searchRequest.search.length - searchRequest.errorTolerance
                && getActualNumberOfErrors() <= searchRequest.errorTolerance
    }

    private fun nextStates(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        return buildFinisherState(nextNode)
            ?: buildMatchState(nextNode)
            ?: buildErrorState(nextNode)
            ?: buildResetState(nextNode)
    }

    private fun buildFinisherState(nextNode: TrieNode<T>): List<FuzzySubstringSearchState<T>>? {
        if (!sufficientlyMatches()) {
            return null
        }

        val nextNodeMatches = searchCoordinatesMatch(nextNode)

        val newSearchIndex = min(searchRequest.search.length, searchCoordinates.searchIndex + 1)

        val newNumberOfErrors = when {
            searchCoordinates.searchIndex < searchRequest.search.length && !nextNodeMatches ->
                searchCoordinates.numberOfErrors + 1
            else ->
                searchCoordinates.numberOfErrors
        }

        val newNumberOfMatches = when {
            nextNodeMatches -> searchCoordinates.numberOfMatches + 1
            else -> searchCoordinates.numberOfMatches
        }

        val newEndMatchIndex = when {
            nextNodeMatches -> searchVariables.sequence.length
            searchCoordinates.endMatchIndex == null -> searchVariables.sequence.length - 1
            else -> searchCoordinates.endMatchIndex
        }

        val finisherStates = mutableListOf(
            FuzzySubstringSearchState(
                searchRequest = searchRequest,
                searchVariables = SearchVariables(
                    node = nextNode,
                    sequence = StringBuilder(searchVariables.sequence).append(nextNode.string),
                    isFinisherState = true,
                ),
                searchCoordinates = SearchCoordinates(
                    startMatchIndex = searchCoordinates.startMatchIndex,
                    endMatchIndex = newEndMatchIndex,
                    searchIndex = newSearchIndex,
                    numberOfMatches = newNumberOfMatches,
                    numberOfErrors = newNumberOfErrors,
                )
            )
        )

        // in case we find a better match further in the string
        val perfectMatch = searchRequest.search.length == searchCoordinates.numberOfMatches
        if (!searchVariables.isFinisherState && !perfectMatch) {
            finisherStates.addAll(buildResetState(nextNode))
        }

        return finisherStates
    }

    private fun buildMatchState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0

        val matchingPreconditions = when (searchRequest.matchingStrategy) {
            FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
            FuzzySubstringMatchingStrategy.MATCH_PREFIX ->
                wasMatchingBefore || searchVariables.node.string.isWordSeparator()
            else ->
                true
        }

        val nextNodeMatches = matchingPreconditions && searchCoordinatesMatch(nextNode)

        if (nextNodeMatches) {
            return listOf(
                FuzzySubstringSearchState(
                    searchRequest = searchRequest,
                    searchVariables = SearchVariables(
                        node = nextNode,
                        sequence = StringBuilder(searchVariables.sequence).append(nextNode.string),
                        isFinisherState = false
                    ),
                    searchCoordinates = SearchCoordinates(
                        startMatchIndex = searchCoordinates.startMatchIndex ?: searchVariables.sequence.length,
                        endMatchIndex = searchVariables.sequence.length,
                        searchIndex = searchCoordinates.searchIndex + 1,
                        numberOfMatches = searchCoordinates.numberOfMatches + 1,
                        numberOfErrors = searchCoordinates.numberOfErrors,
                    )
                )
            )
        } else {
            return null
        }
    }

    private fun searchCoordinatesMatch(nextNode: TrieNode<T>): Boolean {
        return searchCoordinates.searchIndex < searchRequest.search.length
                && nextNode.string == searchRequest.search[searchCoordinates.searchIndex].toString()
    }

    private fun buildErrorState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0

        val hasErrorAllowance = searchCoordinates.numberOfErrors < searchRequest.errorTolerance

        val shouldContinueMatchingWithError = hasErrorAllowance
                && when (searchRequest.matchingStrategy) {
                    FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                        wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
                    else ->
                        wasMatchingBefore
                }

        if (shouldContinueMatchingWithError) {
            return getErrorStrategies(nextNode).mapIndexed { i, it ->
                FuzzySubstringSearchState(
                    searchRequest = searchRequest,
                    searchVariables = SearchVariables(
                        node = it.node,
                        sequence = it.sequence,
                        isFinisherState = false
                    ),
                    searchCoordinates = SearchCoordinates(
                        startMatchIndex = searchCoordinates.startMatchIndex,
                        endMatchIndex = searchCoordinates.endMatchIndex,
                        searchIndex = it.searchIndex,
                        numberOfMatches = searchCoordinates.numberOfMatches,
                        numberOfErrors = searchCoordinates.numberOfErrors + 1,
                    )
                )
            }
        } else {
            return null
        }
    }

    private fun getErrorStrategies(nextNode: TrieNode<T>): List<SearchWithErrorStrategy<T>> {
        val strategies = mutableListOf<SearchWithErrorStrategy<T>>()

        if (searchCoordinates.searchIndex + 1 < searchRequest.search.length) {
            strategies.add(
                // 1. misspelling: increment searchIndex and go to the next node
                SearchWithErrorStrategy(
                    nextNode,
                    searchCoordinates.searchIndex + 1,
                    StringBuilder(searchVariables.sequence).append(nextNode.string)
                )
            )

            strategies.add(
                // 2. missing letter in data: increment searchIndex and stay at the previous node
                SearchWithErrorStrategy(
                    searchVariables.node,
                    searchCoordinates.searchIndex + 1,
                    StringBuilder(searchVariables.sequence)
                )
            )
        }

        strategies.add(
            // 3. missing letter in search keyword: keep searchIndex the same and go to the next node
            SearchWithErrorStrategy(
                nextNode,
                searchCoordinates.searchIndex,
                StringBuilder(searchVariables.sequence).append(nextNode.string)
            )
        )

        return strategies
    }

    private fun buildResetState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        return listOf(
            FuzzySubstringSearchState(
                searchRequest = searchRequest,
                searchVariables = SearchVariables(
                    node = nextNode,
                    sequence = StringBuilder(searchVariables.sequence).append(nextNode.string),
                    isFinisherState = false
                ),
                searchCoordinates = SearchCoordinates(
                    startMatchIndex = null,
                    endMatchIndex = null,
                    searchIndex = 0,
                    numberOfMatches = 0,
                    numberOfErrors = 0,
                )
            )
        )
    }

    private fun getActualNumberOfErrors(): Int {
        val unmatchedCharacters = searchRequest.search.length - searchCoordinates.searchIndex
        assert(unmatchedCharacters >= 0)
        return searchRequest.numberOfPredeterminedErrors +
                searchCoordinates.numberOfErrors +
                unmatchedCharacters
    }

    private fun distanceToStartWordSeparatorIsPermissible(): Boolean {
        val indexOfLastWordSeparator = searchVariables.sequence.indexOfLastWordSeparator() ?: -1
        val distanceToWordSeparator = searchVariables.sequence.length - 1 - indexOfLastWordSeparator
        return distanceToWordSeparator - 1 <= searchCoordinates.numberOfErrors
    }

    private fun matchedWholeSequence(startMatchIndex: Int, endMatchIndex: Int): Boolean {
        return startMatchIndex == 0 && endMatchIndex >= searchVariables.sequence.length - 1
    }

    private fun matchedWholeWord(startMatchIndex: Int, endMatchIndex: Int): Boolean {
        return searchVariables.sequence.isWordSeparatorAt(startMatchIndex - 1)
                && searchVariables.sequence.isWordSeparatorAt(endMatchIndex + 1)
    }

    private fun StringBuilder.isWordSeparatorAt(index: Int): Boolean {
        return index < 0 || index >= this.length || this[index].toString().isWordSeparator()
    }

    private fun String.isWordSeparator(): Boolean {
        return this == "" || this.matches(wordSeparatorRegex)
    }

    private fun CharSequence.indexOfLastWordSeparator(endIndex: Int = this.length - 1): Int? {
        return (0..endIndex).reversed().firstOrNull {
            this[it].toString().matches(wordSeparatorRegex)
        }
    }

    private fun CharSequence.indexOfFirstWordSeparator(startIndex: Int = 0): Int? {
        return (startIndex..<this.length).firstOrNull {
            this[it].toString().matches(wordSeparatorRegex)
        }
    }

    companion object {

        /**
         * Emulates a public constructor, keeping some properties private as part of
         * this class's implementation details
         */
        operator fun <T> invoke(
            root: TrieNode<T>,
            search: String,
            numberOfPredeterminedErrors: Int,
            errorTolerance: Int,
            matchingStrategy: FuzzySubstringMatchingStrategy
        ): FuzzySubstringSearchState<T> {

            return FuzzySubstringSearchState(
                searchRequest = SearchRequest(
                    matchingStrategy = matchingStrategy,
                    search = search,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                ),
                searchVariables = SearchVariables(
                    node = root,
                    sequence = StringBuilder(),
                    isFinisherState = false,
                ),
                searchCoordinates = SearchCoordinates(0, 0, 0, null, null)
            )
        }
    }
}