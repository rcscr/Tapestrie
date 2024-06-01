package com.rcs.trie

import kotlin.math.max

class FuzzySubstringSearchState<T> private constructor(
    private val searchInvariables: SearchInvariables,
    private val node: TrieNode<T>,
    private val nextNodeToSkip: TrieNode<T>?,
    private val sequence: StringBuilder,
    private val isFinisherState: Boolean,
    private val searchCoordinates: SearchCoordinates
) {

    data class SearchInvariables(
        val matchingStrategy: FuzzySubstringMatchingStrategy,
        val search: String,
        val numberOfPredeterminedErrors: Int,
        val errorTolerance: Int,
    )

    data class SearchCoordinates(
        val searchIndex: Int,
        val numberOfMatches: Int,
        val numberOfErrors: Int,
        val startMatchIndex: Int?,
        var endMatchIndex: Int?,
    )

    data class SearchWithErrorStrategy<T>(
        val node: TrieNode<T>,
        val nextNodeToSkip: TrieNode<T>?,
        val searchIndex: Int,
        val sequence: StringBuilder
    )

    private val wordSeparatorRegex = "[\\s\\p{P}]".toRegex()

    fun nextStates(): Collection<FuzzySubstringSearchState<T>> {
        synchronized(node.next) {
            return node.next
                .filter { nextNodeToSkip == null || it != nextNodeToSkip }
                .map { nextStates(it) }
                .flatten()
        }
    }

    fun hasSearchResult(): Boolean {
        return node.completes() && sufficientlyMatches()
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

        val indexOfWordSeparatorBefore = sequence
            .indexOfLastWordSeparator(assertedStartMatchIndex) ?: -1

        val indexOfWordSeparatorAfter = sequence
            .indexOfFirstWordSeparator(assertedStartMatchIndex) ?: sequence.length

        val prefixDistance = assertedStartMatchIndex - indexOfWordSeparatorBefore - 1

        val matchedSubstring = sequence.substring(assertedStartMatchIndex, assertedEndMatchIndex + 1)

        val matchedWord = sequence.substring(indexOfWordSeparatorBefore + 1, indexOfWordSeparatorAfter)

        return TrieSearchResult(
            sequence.toString(),
            node.value!!,
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
                && searchCoordinates.numberOfMatches >= searchInvariables.search.length - searchInvariables.errorTolerance
                && getActualNumberOfErrors() <= searchInvariables.errorTolerance
    }

    private fun nextStates(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        return buildFinisherState(nextNode)
            ?: buildSearchMatchState(nextNode)
            ?: buildSearchErrorState(nextNode)
            ?: buildSearchResetState(nextNode)
    }

    private fun buildFinisherState(nextNode: TrieNode<T>): List<FuzzySubstringSearchState<T>>? {
        if (!sufficientlyMatches()) {
            return null
        }

        val nextNodeMatches = searchCoordinatesMatch(nextNode)

        val newSearchIndex = searchCoordinates.searchIndex + 1

        val newNumberOfErrors = when {
            searchCoordinates.searchIndex < searchInvariables.search.length && !nextNodeMatches ->
                searchCoordinates.numberOfErrors + 1
            else ->
                searchCoordinates.numberOfErrors
        }

        val newNumberOfMatches = when {
            nextNodeMatches -> searchCoordinates.numberOfMatches + 1
            else -> searchCoordinates.numberOfMatches
        }

        val newEndMatchIndex = when {
            nextNodeMatches -> sequence.length
            searchCoordinates.endMatchIndex == null -> sequence.length - 1
            else -> searchCoordinates.endMatchIndex
        }

        val finisherStates = mutableListOf(
            FuzzySubstringSearchState(
                searchInvariables = searchInvariables,
                node = nextNode,
                nextNodeToSkip = null,
                sequence = StringBuilder(sequence).append(nextNode.string),
                isFinisherState = true,
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
        if (!isFinisherState && searchInvariables.search.length != searchCoordinates.numberOfMatches) {
            finisherStates.addAll(buildSearchResetState(nextNode))
        }

        return finisherStates
    }

    private fun buildSearchMatchState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0

        val matchingPreconditions = when (searchInvariables.matchingStrategy) {
            FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
            FuzzySubstringMatchingStrategy.MATCH_PREFIX ->
                wasMatchingBefore || node.string.isWordSeparator()
            else ->
                true
        }

        val nextNodeMatches = matchingPreconditions && searchCoordinatesMatch(nextNode)

        if (nextNodeMatches) {
            return listOf(
                FuzzySubstringSearchState(
                    searchInvariables = searchInvariables,
                    node = nextNode,
                    nextNodeToSkip = null,
                    sequence = StringBuilder(sequence).append(nextNode.string),
                    isFinisherState = false,
                    searchCoordinates = SearchCoordinates(
                        startMatchIndex = searchCoordinates.startMatchIndex ?: sequence.length,
                        endMatchIndex = sequence.length,
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
        return searchCoordinates.searchIndex < searchInvariables.search.length
                && nextNode.string == searchInvariables.search[searchCoordinates.searchIndex].toString()
    }

    private fun buildSearchErrorState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0

        val hasErrorAllowance = searchCoordinates.numberOfErrors < searchInvariables.errorTolerance

        val shouldContinueMatchingWithError = hasErrorAllowance
                && when (searchInvariables.matchingStrategy) {
                    FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                        wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
                    else ->
                        wasMatchingBefore
                }

        if (shouldContinueMatchingWithError) {
            return getErrorStrategies(nextNode).map {
                FuzzySubstringSearchState(
                    searchInvariables = searchInvariables,
                    node = it.node,
                    nextNodeToSkip = it.nextNodeToSkip,
                    sequence = it.sequence,
                    isFinisherState = false,
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
        return listOf(
            // 1. misspelling: increment searchIndex and go to the next node
            SearchWithErrorStrategy(
                nextNode,
                null,
                searchCoordinates.searchIndex + 1,
                StringBuilder(sequence).append(nextNode.string)),

            // 2. missing letter in data: increment searchIndex and stay at the previous node
            SearchWithErrorStrategy(
                node,
                // Optimization: if any node in this.node.next matches current string,
                // do not continue this error search strategy at that node
                nextNodeToSkip
                    ?: node.next.firstOrNull {
                        searchCoordinates.searchIndex < searchInvariables.search.length
                                && it.string == searchInvariables.search[searchCoordinates.searchIndex].toString()
                    },
                searchCoordinates.searchIndex + 1,
                StringBuilder(sequence)),

            // 3. missing letter in search keyword: keep searchIndex the same and go to the next node
            SearchWithErrorStrategy(
                nextNode,
                null,
                searchCoordinates.searchIndex,
                StringBuilder(sequence).append(nextNode.string)),
        )
    }

    private fun buildSearchResetState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        return listOf(
            FuzzySubstringSearchState(
                searchInvariables = searchInvariables,
                node = nextNode,
                nextNodeToSkip = null,
                sequence = StringBuilder(sequence).append(nextNode.string),
                isFinisherState = false,
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
        val unmatchedCharacters = max(0, searchInvariables.search.length - searchCoordinates.searchIndex)
        return searchInvariables.numberOfPredeterminedErrors +
                searchCoordinates.numberOfErrors +
                unmatchedCharacters
    }

    private fun distanceToStartWordSeparatorIsPermissible(): Boolean {
        val indexOfLastWordSeparator = sequence.indexOfLastWordSeparator() ?: -1
        val distanceToWordSeparator = sequence.length - 1 - indexOfLastWordSeparator
        return distanceToWordSeparator - 1 <= searchCoordinates.numberOfErrors
    }

    private fun matchedWholeSequence(startMatchIndex: Int, endMatchIndex: Int): Boolean {
        return startMatchIndex == 0 && endMatchIndex >= sequence.length - 1
    }

    private fun matchedWholeWord(startMatchIndex: Int, endMatchIndex: Int): Boolean {
        return sequence.isWordSeparatorAt(startMatchIndex - 1)
                && sequence.isWordSeparatorAt(endMatchIndex + 1)
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
                searchInvariables = SearchInvariables(
                    matchingStrategy = matchingStrategy,
                    search = search,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                ),
                node = root,
                nextNodeToSkip = null,
                sequence = StringBuilder(),
                isFinisherState = false,
                searchCoordinates = SearchCoordinates(0, 0, 0, null, null)
            )
        }
    }
}