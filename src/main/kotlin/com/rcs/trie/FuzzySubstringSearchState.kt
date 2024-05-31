package com.rcs.trie

class FuzzySubstringSearchState<T> private constructor(
    private val matchingStrategy: FuzzySubstringMatchingStrategy,
    private val search: String,
    private val node: TrieNode<T>,
    private val startMatchIndex: Int?,
    private var endMatchIndex: Int?,
    private val searchIndex: Int,
    private val numberOfMatches: Int,
    private val numberOfErrors: Int,
    private val numberOfPredeterminedErrors: Int,
    private val errorTolerance: Int,
    private val sequence: StringBuilder,
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

    private val wordSeparatorRegex = "[\\s\\p{P}]".toRegex()

    fun completes(): Boolean {
        return node.completes()
    }

    /**
     * Returns true if this state *sufficiently* matches.
     * This does not necessarily mean that the matching is finished;
     * it is possible that more matching characters will be found
     * when calling state.nextBuildState next.
     */
    fun sufficientlyMatches(): Boolean {
        return startMatchIndex != null
                && (node.completes() || searchIndex == search.length)
                && numberOfMatches >= search.length - errorTolerance
                && getActualNumberOfErrors() <= errorTolerance
    }

    fun nextSearchStates(): Collection<FuzzySubstringSearchState<T>> {
        synchronized(node.next) {
            return node.next.map { nextSearchStates(it) }.flatten()
        }
    }

    fun nextBuildStates(): Collection<FuzzySubstringSearchState<T>> {
        synchronized(node.next) {
            return node.next.map { nextBuildState(it) }
        }
    }

    fun buildSearchResult(): TrieSearchResult<T> {
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
        buildSearchMatchState(nextNode)?.let { return it }
        buildSearchErrorState(nextNode)?.let { return it }
        return buildSearchResetState(nextNode)
    }

    private fun nextBuildState(nextNode: TrieNode<T>): FuzzySubstringSearchState<T> {
        val nextNodeMatches = searchIndex < search.length
                && nextNode.string == search[searchIndex].toString()

        val newSearchIndex =
            if (nextNodeMatches) searchIndex + 1
            else searchIndex

        val newNumberOfMatches =
            if (nextNodeMatches) numberOfMatches + 1
            else numberOfMatches

        val newEndMatchIndex =
            if (nextNodeMatches) sequence.length
            else if (endMatchIndex == null) sequence.length - 1
            else endMatchIndex

        return FuzzySubstringSearchState(
            matchingStrategy = matchingStrategy,
            search = search,
            node = nextNode,
            startMatchIndex = startMatchIndex,
            endMatchIndex = newEndMatchIndex,
            searchIndex = newSearchIndex,
            numberOfMatches = newNumberOfMatches,
            numberOfErrors = numberOfErrors,
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

        // happy path - continue matching
        if (nextNodeMatches) {
            return listOf(
                FuzzySubstringSearchState(
                    matchingStrategy = matchingStrategy,
                    search = search,
                    node = nextNode,
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
        // there are three ways this can go: 1. misspelling, 2. missing letter in search input 3. missing letter in data
        if (shouldContinueMatchingWithError) {
            data class SearchWithErrorStrategy(
                val node: TrieNode<T>,
                val searchIndex: Int,
                val sequence: StringBuilder
            )

            val errorSearchStrategies = listOf(
                // 1. misspelling
                // increment searchIndex and go to the next node
                SearchWithErrorStrategy(nextNode, searchIndex + 1, StringBuilder(sequence).append(nextNode.string)),

                // 2. missing letter in target data
                // increment searchIndex and stay at the previous node
                SearchWithErrorStrategy(node, searchIndex + 1, StringBuilder(sequence)),

                // 3. missing letter in search input
                // keep searchIndex the same and go to the next node
                SearchWithErrorStrategy(nextNode, searchIndex, StringBuilder(sequence).append(nextNode.string)),
            )

            return errorSearchStrategies.map {
                FuzzySubstringSearchState(
                    matchingStrategy = matchingStrategy,
                    search = search,
                    node = it.node,
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

    private fun buildSearchResetState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        return listOf(
            FuzzySubstringSearchState(
                matchingStrategy = matchingStrategy,
                search = search,
                node = nextNode,
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
        val unmatchedCharacters = search.length - searchIndex
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
        for (i in (0..endIndex).reversed()) {
            if (this[i].toString().matches(wordSeparatorRegex)) {
                return i
            }
        }
        return null
    }

    private fun CharSequence.indexOfFirstWordSeparator(startIndex: Int = 0): Int? {
        for (i in startIndex..<this.length) {
            if (this[i].toString().matches(wordSeparatorRegex)) {
                return i
            }
        }
        return null
    }
}