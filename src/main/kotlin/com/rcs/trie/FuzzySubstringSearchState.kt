package com.rcs.trie

data class FuzzySubstringSearchState<T>(
    val search: String,
    val node: Node<T>,
    val leftOfFirstMatchingCharacter: Node<T>?,
    var rightOfLastMatchingCharacter: Node<T>?,
    val searchIndex: Int,
    val numberOfMatches: Int,
    val numberOfErrors: Int,
    val errorTolerance: Int,
    val sequence: StringBuilder,
) {
    private val wholeWordSeparator = "[\\s\\p{P}]"

    fun matches(): Boolean {
        val match = searchIndex >= search.length
                && numberOfMatches + numberOfErrors >= search.length - numberOfErrors
                && numberOfErrors <= errorTolerance

        val partialMatch = node.completes()
                && numberOfMatches >= search.length - errorTolerance

        return match || partialMatch
    }

    fun nextBuildState(nextNode: Node<T>): FuzzySubstringSearchState<T> {
        val endMatch = null != rightOfLastMatchingCharacter
        
        val nextNodeMatches = !endMatch && numberOfMatches < search.length
                && nextNode.string == search[numberOfMatches].toString()

        val nextRightOfLastMatchingCharacter =
            if (!endMatch && !nextNodeMatches) nextNode
            else rightOfLastMatchingCharacter

        val newNumberOfMatches =
            if (!endMatch && nextNodeMatches) numberOfMatches + 1
            else numberOfMatches

        return FuzzySubstringSearchState(
            search = search,
            node = nextNode,
            leftOfFirstMatchingCharacter = leftOfFirstMatchingCharacter,
            rightOfLastMatchingCharacter = nextRightOfLastMatchingCharacter,
            searchIndex = searchIndex,
            numberOfMatches = newNumberOfMatches,
            numberOfErrors = numberOfErrors,
            errorTolerance = errorTolerance,
            sequence = StringBuilder(sequence).append(nextNode.string)
        )
    }

    fun nextSearchStates(nextNode: Node<T>): Collection<FuzzySubstringSearchState<T>> {
        val nextStates = mutableListOf<FuzzySubstringSearchState<T>>()

        val nextNodeMatches = nextNode.string == search[searchIndex].toString()

        if (nextNodeMatches) {
            // happy path - continue matching
            nextStates.add(
                FuzzySubstringSearchState(
                    search = search,
                    node = nextNode,
                    leftOfFirstMatchingCharacter = leftOfFirstMatchingCharacter ?: node,
                    rightOfLastMatchingCharacter = null,
                    searchIndex = searchIndex + 1,
                    numberOfMatches = numberOfMatches + 1,
                    numberOfErrors = numberOfErrors,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence).append(nextNode.string)
                )
            )
        } else if (numberOfMatches > 0 && numberOfErrors < errorTolerance) {
            // was matching before, but no longer matches
            // however, there's some error tolerance to be used
            // there are three ways this can go: misspelling, missing letter in search input, or missing letter in data

            // misspelling
            // increment searchIndex and go to the next node
            if (searchIndex + 1 < search.length) {
                nextStates.add(
                    FuzzySubstringSearchState(
                        search = search,
                        node = nextNode,
                        leftOfFirstMatchingCharacter = leftOfFirstMatchingCharacter,
                        rightOfLastMatchingCharacter = null,
                        searchIndex = searchIndex + 1,
                        numberOfMatches = numberOfMatches,
                        numberOfErrors = numberOfErrors + 1,
                        errorTolerance = errorTolerance,
                        sequence = StringBuilder(sequence).append(nextNode.string)
                    )
                )
            }

            // missing letter in data
            // increment searchIndex and go to the previous node
            if (searchIndex + 1 < search.length) {
                nextStates.add(
                    FuzzySubstringSearchState(
                        search = search,
                        node = node,
                        leftOfFirstMatchingCharacter = leftOfFirstMatchingCharacter,
                        rightOfLastMatchingCharacter = null,
                        searchIndex = searchIndex + 1,
                        numberOfMatches = numberOfMatches,
                        numberOfErrors = numberOfErrors + 1,
                        errorTolerance = errorTolerance,
                        sequence = StringBuilder(sequence)
                    )
                )
            }

            // missing letter in search input
            // keep searchIndex the same and go to the next node
            nextStates.add(
                FuzzySubstringSearchState(
                    search = search,
                    node = nextNode,
                    leftOfFirstMatchingCharacter = leftOfFirstMatchingCharacter,
                    rightOfLastMatchingCharacter = null,
                    searchIndex = searchIndex,
                    numberOfMatches = numberOfMatches,
                    numberOfErrors = numberOfErrors + 1,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence).append(nextNode.string)
                )
            )
        } else {
            // reset matching
            nextStates.add(
                FuzzySubstringSearchState(
                    search = search,
                    node = nextNode,
                    leftOfFirstMatchingCharacter = null,
                    rightOfLastMatchingCharacter = null,
                    searchIndex = 0,
                    numberOfMatches = 0,
                    numberOfErrors = 0,
                    errorTolerance = errorTolerance,
                    sequence = StringBuilder(sequence).append(nextNode.string)
                )
            )
        }
        
        return nextStates
    }

    fun buildSearchResult(): TrieSearchResult<T> {
        assert(node.completes())

        val sequenceString = sequence.toString()

        val actualErrors =
            if (sequenceString.length < search.length) search.length - numberOfMatches
            else numberOfErrors

        val matchedWholeSequence = actualErrors == 0
                && matchedWholeSequence(leftOfFirstMatchingCharacter!!, rightOfLastMatchingCharacter)

        val matchedWholeWord = actualErrors == 0
                && matchedWholeWord(leftOfFirstMatchingCharacter!!, rightOfLastMatchingCharacter)

        return TrieSearchResult(
            sequenceString,
            node.value!!,
            numberOfMatches,
            actualErrors,
            matchedWholeSequence,
            matchedWholeWord
        )
    }

    private fun matchedWholeSequence(
        leftOfFirstMatchingCharacter: Node<T>,
        rightOfLastMatchingCharacter: Node<T>?
    ): Boolean {
        return leftOfFirstMatchingCharacter.string == ""
                && rightOfLastMatchingCharacter == null
    }

    private fun matchedWholeWord(
        leftOfFirstMatchingCharacter: Node<T>,
        rightOfLastMatchingCharacter: Node<T>?
    ): Boolean {
        return leftOfFirstMatchingCharacter.isWordSeparator()
                && rightOfLastMatchingCharacter.isWordSeparator()
    }

    private fun Node<T>?.isWordSeparator(): Boolean {
        return this == null || this.string == "" || this.string.matches(wholeWordSeparator.toRegex())
    }
}