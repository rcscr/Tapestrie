package com.rcs.trie

data class FuzzySubstringSearchState<T>(
    val search: String,
    val node: TrieNode<T>,
    val startMatchIndex: Int?,
    val endMatchIndex: Int?,
    val searchIndex: Int,
    val numberOfMatches: Int,
    val numberOfErrors: Int,
    val numberOfPredeterminedErrors: Int,
    val errorTolerance: Int,
    val sequence: StringBuilder,
) {
    private val wordSeparatorRegex = "[\\s\\p{P}]".toRegex()

    fun sufficientlyMatches(): Boolean {
        return startMatchIndex != null
                && (node.completes() || searchIndex == search.length)
                && numberOfMatches >= search.length - errorTolerance
                && getActualNumberOfErrors() <= errorTolerance
    }

    fun nextBuildState(nextNode: TrieNode<T>): FuzzySubstringSearchState<T> {
        val matchHasEnded = endMatchIndex != null

        val nextNodeMatches = !matchHasEnded
                && searchIndex < search.length
                && nextNode.string == search[searchIndex].toString()

        val newSearchIndex =
            if (nextNodeMatches) searchIndex + 1
            else searchIndex

        val newNumberOfMatches =
            if (nextNodeMatches) numberOfMatches + 1
            else numberOfMatches

        val newEndMatchIndex =
            if (matchHasEnded) endMatchIndex
            else if (!nextNodeMatches) sequence.length - 1
            else null

        return FuzzySubstringSearchState(
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

    fun nextSearchStates(
        nextNode: TrieNode<T>,
        matchingStrategy: FuzzySubstringMatchingStrategy
    ): Collection<FuzzySubstringSearchState<T>> {

        val wasMatchingBefore = numberOfMatches > 0

        val matchingPreconditions = when (matchingStrategy) {
            FuzzySubstringMatchingStrategy.LIBERAL ->
                true
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
                    search = search,
                    node = nextNode,
                    startMatchIndex = startMatchIndex ?: sequence.length,
                    endMatchIndex = null,
                    searchIndex = searchIndex + 1,
                    numberOfMatches = numberOfMatches + 1,
                    numberOfErrors = numberOfErrors,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence).append(nextNode.string)
                )
            )
        }

        val shouldContinueMatchingWithError = numberOfErrors < errorTolerance
                && when(matchingStrategy) {
                    FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                        distanceToStartWordSeparatorIsPermissible()
                    else ->
                        wasMatchingBefore
                }

        // No longer matches - however, there's some error tolerance to be used
        // there are three ways this can go: 1. misspelling, 2. missing letter in search input 3. missing letter in data
        if (shouldContinueMatchingWithError) {
            val nextStates = mutableListOf<FuzzySubstringSearchState<T>>()

            // 1. misspelling
            // increment searchIndex and go to the next node
            nextStates.add(
                FuzzySubstringSearchState(
                    search = search,
                    node = nextNode,
                    startMatchIndex = startMatchIndex,
                    endMatchIndex = null,
                    searchIndex = searchIndex + 1,
                    numberOfMatches = numberOfMatches,
                    numberOfErrors = numberOfErrors + 1,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence).append(nextNode.string)
                )
            )

            // 2. missing letter in target data
            // increment searchIndex and stay at the previous node
            nextStates.add(
                FuzzySubstringSearchState(
                    search = search,
                    node = node,
                    startMatchIndex = startMatchIndex,
                    endMatchIndex = null,
                    searchIndex = searchIndex + 1,
                    numberOfMatches = numberOfMatches,
                    numberOfErrors = numberOfErrors + 1,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence)
                )
            )

            // 2. missing letter in search input
            // keep searchIndex the same and go to the next node
            nextStates.add(
                FuzzySubstringSearchState(
                    search = search,
                    node = nextNode,
                    startMatchIndex = startMatchIndex,
                    endMatchIndex = null,
                    searchIndex = searchIndex,
                    numberOfMatches = numberOfMatches,
                    numberOfErrors = numberOfErrors + 1,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence).append(nextNode.string)
                )
            )

            return nextStates
        }

        // exhausted all attempts; reset matching
        return listOf(
            FuzzySubstringSearchState(
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

    fun buildSearchResult(): TrieSearchResult<T> {
        val actualErrors = getActualNumberOfErrors()

        val actualEndMatchIndex = endMatchIndex ?: (sequence.length - 1)

        val matchedWholeSequence = actualErrors == 0
                && matchedWholeSequence(startMatchIndex!!, actualEndMatchIndex)

        val matchedWholeWord = actualErrors == 0
                && matchedWholeWord(startMatchIndex!!, actualEndMatchIndex)

        val indexOfWordSeparatorBefore = sequence.subSequence(0, startMatchIndex!! + 1)
            .indexOfLastWordSeparator() ?: -1

        val relativeIndexOfWordSeparatorAfter = sequence.subSequence(startMatchIndex, sequence.length)
            .indexOfFirstWordSeparator() ?: (sequence.length - startMatchIndex)

        val indexOfWordSeparatorAfter = startMatchIndex + relativeIndexOfWordSeparatorAfter

        val prefixDistance = startMatchIndex - indexOfWordSeparatorBefore - 1

        val matchedSubstring = sequence.substring(startMatchIndex, actualEndMatchIndex + 1)

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

    private fun CharSequence.indexOfLastWordSeparator(): Int? {
        for (i in this.indices.reversed()) {
            if (this[i].toString().matches(wordSeparatorRegex)) {
                return i
            }
        }
        return null
    }

    private fun CharSequence.indexOfFirstWordSeparator(): Int? {
        for (i in this.indices) {
            if (this[i].toString().matches(wordSeparatorRegex)) {
                return i
            }
        }
        return null
    }
}