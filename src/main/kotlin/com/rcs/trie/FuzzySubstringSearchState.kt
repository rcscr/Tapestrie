package com.rcs.trie

data class FuzzySubstringSearchState<T>(
    val matchingStrategy: FuzzySubstringMatchingStrategy,
    val search: String,
    val node: TrieNode<T>,
    val startMatchIndex: Int?,
    var endMatchIndex: Int?,
    val searchIndex: Int,
    val numberOfMatches: Int,
    val numberOfErrors: Int,
    val numberOfPredeterminedErrors: Int,
    val errorTolerance: Int,
    val sequence: StringBuilder,
) {
    private val wordSeparatorRegex = "[\\s\\p{P}]".toRegex()

    /**
     * This function returns true if this state *sufficiently* matches
     * this does not mean that the matching is finished
     * it is possible that more matching characters will be found
     * when subsequently calling state.nextBuildState
     */
    fun sufficientlyMatches(): Boolean {
        return startMatchIndex != null
                && (node.completes() || searchIndex == search.length)
                && numberOfMatches >= search.length - errorTolerance
                && getActualNumberOfErrors() <= errorTolerance
    }

    fun nextBuildState(nextNode: TrieNode<T>): FuzzySubstringSearchState<T> {
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

    fun nextSearchStates(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>> {
        val wasMatchingBefore = numberOfMatches > 0

        val matchingPreconditions = when (matchingStrategy) {
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
        }

        val shouldContinueMatchingWithError = numberOfErrors < errorTolerance
                && when(matchingStrategy) {
                    FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX ->
                        wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
                    else ->
                        wasMatchingBefore
                }

        // No longer matches - however, there's some error tolerance to be used
        // there are three ways this can go: 1. misspelling, 2. missing letter in search input 3. missing letter in data
        if (shouldContinueMatchingWithError) {
            val errorSearchStrategies = listOf(
                // 1. misspelling
                // increment searchIndex and go to the next node
                Triple(nextNode, searchIndex + 1, StringBuilder(sequence).append(nextNode.string)),

                // 2. missing letter in target data
                // increment searchIndex and stay at the previous node
                Triple(node, searchIndex + 1, StringBuilder(sequence)),

                // 3. missing letter in search input
                // keep searchIndex the same and go to the next node
                Triple(nextNode, searchIndex, StringBuilder(sequence).append(nextNode.string)),
            )

            return errorSearchStrategies.map {
                FuzzySubstringSearchState(
                    matchingStrategy = matchingStrategy,
                    search = search,
                    node = it.first,
                    startMatchIndex = startMatchIndex,
                    endMatchIndex = endMatchIndex,
                    searchIndex = it.second,
                    numberOfMatches = numberOfMatches,
                    numberOfErrors = numberOfErrors + 1,
                    numberOfPredeterminedErrors = numberOfPredeterminedErrors,
                    errorTolerance = errorTolerance,
                    sequence = it.third
                )
            }
        }

        // exhausted all attempts; reset matching
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