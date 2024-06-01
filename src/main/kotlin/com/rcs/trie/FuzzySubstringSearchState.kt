package com.rcs.trie

import kotlin.math.max

class FuzzySubstringSearchState<T> private constructor(
    private val matchingStrategy: FuzzySubstringMatchingStrategy,
    private val search: String,
    private val node: TrieNode<T>,
    private val nextNodeToSkip: TrieNode<T>?,
    private val startMatchIndex: Int?,
    private var endMatchIndex: Int?,
    private val searchIndex: Int,
    private val numberOfMatches: Int,
    private val numberOfErrors: Int,
    private val numberOfPredeterminedErrors: Int,
    private val errorTolerance: Int,
    private val sequence: StringBuilder
) {

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
                matchingStrategy = matchingStrategy,
                search = search,
                node = root,
                nextNodeToSkip = null,
                startMatchIndex = null,
                endMatchIndex =  null,
                searchIndex = 0,
                numberOfMatches = 0,
                numberOfErrors = 0,
                numberOfPredeterminedErrors,
                errorTolerance,
                sequence = StringBuilder()
            )
        }
    }

    data class SearchWithErrorStrategy<T>(
        val node: TrieNode<T>,
        val nextNodeToSkip: TrieNode<T>?,
        val searchIndex: Int,
        val sequence: StringBuilder
    )

    private val wordSeparatorRegex = "[\\s\\p{P}]".toRegex()

    fun completes(): Boolean {
        return node.completes()
    }

    /**
     * Returns true if this state *sufficiently* matches.
     *
     * For the string ending at this.node (assuming this.node.completes()),
     * the matching is finished and the result can be obtained via this.buildSearchResult().
     *
     * For other strings that stem from this.node.next, it is possible that more
     * matching characters will be found when subsequently calling state.nextBuildState.
     */
    fun sufficientlyMatches(): Boolean {
        return startMatchIndex != null
                && (node.completes() || searchIndex == search.length)
                && numberOfMatches >= search.length - errorTolerance
                && getActualNumberOfErrors() <= errorTolerance
    }

    fun nextSearchStates(): Collection<FuzzySubstringSearchState<T>> {
        synchronized(node.next) {
            return node.next
                .filter {
                    nextNodeToSkip == null || it != nextNodeToSkip
                }
                .map { nextSearchStates(it) }
                .flatten()
        }
    }

    fun nextBuildStates(): Collection<FuzzySubstringSearchState<T>> {
        synchronized(node.next) {
            return node.next.map { nextBuildState(it) }
        }
    }

    fun buildSearchResult(): TrieSearchResult<T> {
        assert(completes() && sufficientlyMatches())

        val actualErrors = getActualNumberOfErrors()

        val assertedStartMatchIndex = startMatchIndex!!

        val assertedEndMatchIndex = endMatchIndex!!

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
            numberOfMatches,
            actualErrors,
            prefixDistance,
            matchedWholeSequence,
            matchedWholeWord
        )
    }

    private fun nextSearchStates(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        return buildSearchMatchState(nextNode)
            ?: buildSearchErrorState(nextNode)
            ?: buildSearchResetState(nextNode)
    }

    private fun nextBuildState(nextNode: TrieNode<T>): FuzzySubstringSearchState<T> {
        val nextNodeMatches = searchIndex < search.length
                && nextNode.string == search[searchIndex].toString()

        val newSearchIndex = searchIndex + 1

        val newNumberOfErrors = when {
            searchIndex < search.length && !nextNodeMatches -> numberOfErrors + 1
            else -> numberOfErrors
        }

        val newNumberOfMatches = when {
            nextNodeMatches -> numberOfMatches + 1
            else -> numberOfMatches
        }

        val newEndMatchIndex = when {
            nextNodeMatches -> sequence.length
            endMatchIndex == null -> sequence.length - 1
            else -> endMatchIndex
        }

        return FuzzySubstringSearchState(
            matchingStrategy = matchingStrategy,
            search = search,
            node = nextNode,
            nextNodeToSkip = null,
            startMatchIndex = startMatchIndex,
            endMatchIndex = newEndMatchIndex,
            searchIndex = newSearchIndex,
            numberOfMatches = newNumberOfMatches,
            numberOfErrors = newNumberOfErrors,
            numberOfPredeterminedErrors = numberOfPredeterminedErrors,
            errorTolerance = errorTolerance,
            sequence = StringBuilder(sequence).append(nextNode.string)
        )
    }

    private fun buildSearchMatchState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        val wasMatchingBefore = numberOfMatches > 0

        val matchingPreconditions = when (matchingStrategy) {
            FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
            FuzzySubstringMatchingStrategy.MATCH_PREFIX ->
                wasMatchingBefore || node.string.isWordSeparator()
            else ->
                true
        }

        val nextNodeMatches = matchingPreconditions
                && searchIndex < search.length
                && nextNode.string == search[searchIndex].toString()

        if (nextNodeMatches) {
            return listOf(
                FuzzySubstringSearchState(
                    matchingStrategy = matchingStrategy,
                    search = search,
                    node = nextNode,
                    nextNodeToSkip = null,
                    startMatchIndex = startMatchIndex ?: sequence.length,
                    endMatchIndex = sequence.length,
                    searchIndex = searchIndex + 1,
                    numberOfMatches = numberOfMatches + 1,
                    numberOfErrors = numberOfErrors,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence).append(nextNode.string)
                )
            )
        } else {
            return null
        }
    }

    private fun buildSearchErrorState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        val wasMatchingBefore = numberOfMatches > 0

        val shouldContinueMatchingWithError = numberOfErrors < errorTolerance
                && when (matchingStrategy) {
                    FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                        wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
                    else ->
                        wasMatchingBefore
                }

        // No longer matches - however, there's some error tolerance to be used
        if (shouldContinueMatchingWithError) {
            return getErrorStrategies(nextNode).map {
                FuzzySubstringSearchState(
                    matchingStrategy = matchingStrategy,
                    search = search,
                    node = it.node,
                    nextNodeToSkip = it.nextNodeToSkip,
                    startMatchIndex = startMatchIndex,
                    endMatchIndex = endMatchIndex,
                    searchIndex = it.searchIndex,
                    numberOfMatches = numberOfMatches,
                    numberOfErrors = numberOfErrors + 1,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                    sequence = it.sequence
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
                searchIndex + 1,
                StringBuilder(sequence).append(nextNode.string)),

            // 2. missing letter in data: increment searchIndex and stay at the previous node
            SearchWithErrorStrategy(
                node,
                // Optimization: if any node in this.node.next matches current string,
                // do not continue this error search strategy at that node
                nextNodeToSkip
                    ?: node.next.firstOrNull {
                        searchIndex < search.length && it.string == search[searchIndex].toString()
                    },
                searchIndex + 1,
                StringBuilder(sequence)),

            // 3. missing letter in search keyword: keep searchIndex the same and go to the next node
            SearchWithErrorStrategy(
                nextNode,
                null,
                searchIndex,
                StringBuilder(sequence).append(nextNode.string)),
        )
    }

    private fun buildSearchResetState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        return listOf(
            FuzzySubstringSearchState(
                matchingStrategy = matchingStrategy,
                search = search,
                node = nextNode,
                nextNodeToSkip = null,
                startMatchIndex = null,
                endMatchIndex = null,
                searchIndex = 0,
                numberOfMatches = 0,
                numberOfErrors = 0,
                numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                errorTolerance = errorTolerance,
                sequence = StringBuilder(sequence).append(nextNode.string)
            )
        )
    }

    private fun getActualNumberOfErrors(): Int {
        val unmatchedCharacters = max(0, search.length - searchIndex)
        return numberOfPredeterminedErrors + numberOfErrors + unmatchedCharacters
    }

    private fun distanceToStartWordSeparatorIsPermissible(): Boolean {
        val indexOfLastWordSeparator = sequence.indexOfLastWordSeparator() ?: -1
        val distanceToWordSeparator = sequence.length - 1 - indexOfLastWordSeparator
        return distanceToWordSeparator - 1 <= numberOfErrors
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
}