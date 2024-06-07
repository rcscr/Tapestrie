package com.rcs.trie

class FuzzySubstringSearchState<T> private constructor(
    private val searchRequest: SearchRequest,
    private val searchVariables: SearchVariables<T>,
    private val searchCoordinates: SearchCoordinates
) {

    fun nextStates(): Collection<FuzzySubstringSearchState<T>> {
        synchronized(searchVariables.node.next) {
            return searchVariables.node.next
                .map { nextStates(it) ?: listOf() }
                .flatten()
        }
    }

    fun hasSearchResult(): Boolean {
        return searchVariables.node.completes() && matches()
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

    private fun matches(): Boolean {
        return searchCoordinates.startMatchIndex != null
                && searchCoordinates.endMatchIndex != null
                && hasMinimumNumberOfMatches()
                && getActualNumberOfErrors() <= searchRequest.errorTolerance
    }

    private fun hasMinimumNumberOfMatches(): Boolean {
        return searchCoordinates.numberOfMatches >= searchRequest.search.length - searchRequest.errorTolerance
    }

    private fun nextStates(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        if (shouldCull(nextNode)) {
            return null
        }

        return buildMatchState(nextNode)
            ?: buildErrorState(nextNode)
            ?: buildResetState(nextNode)
            ?: buildGatherState(nextNode)
    }

    private fun shouldCull(nextNode: TrieNode<T>): Boolean {
        val numberOfMatchingCharactersNeeded = searchRequest.search.length -
                searchRequest.errorTolerance -
                searchCoordinates.numberOfMatches

        return nextNode.depth < numberOfMatchingCharactersNeeded
    }

    private fun buildMatchState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        if (searchVariables.isGatherState) {
            return null
        }

        return when (nextNodeMatches(nextNode)) {
            true ->
                listOf(
                    FuzzySubstringSearchState(
                        searchRequest = searchRequest,
                        searchVariables = SearchVariables(
                            node = nextNode,
                            sequence = StringBuilder(searchVariables.sequence).append(nextNode.string),
                            isGatherState = false
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
            else ->
                null
        }
    }

    private fun nextNodeMatches(nextNode: TrieNode<T>): Boolean {
        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0

        val matchingPreconditions = when (searchRequest.matchingStrategy) {
            FuzzySubstringMatchingStrategy.FUZZY_PREFIX ->
                wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
            FuzzySubstringMatchingStrategy.EXACT_PREFIX ->
                wasMatchingBefore || searchVariables.node.string.isWordSeparator()
            else ->
                true
        }

        return matchingPreconditions && searchCoordinatesMatch(nextNode)
    }

    private fun buildErrorState(nextNode: TrieNode<T>): Collection<FuzzySubstringSearchState<T>>? {
        if (searchVariables.isGatherState) {
            return null
        }

        return when (shouldContinueMatchingWithError()) {
            true ->
                getErrorStrategies(nextNode).map {
                    FuzzySubstringSearchState(
                        searchRequest = searchRequest,
                        searchVariables = SearchVariables(
                            node = it.node,
                            sequence = it.sequence,
                            isGatherState = false
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
            else ->
                null
        }
    }

    private fun shouldContinueMatchingWithError(): Boolean {
        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0
        val hasSearchCharacters = searchCoordinates.searchIndex + 1 < searchRequest.search.length
        val hasErrorAllowance = searchCoordinates.numberOfErrors < searchRequest.errorTolerance

        return hasSearchCharacters
                && hasErrorAllowance
                && when (searchRequest.matchingStrategy) {
                    FuzzySubstringMatchingStrategy.FUZZY_POSTFIX ->
                        hasMinimumNumberOfMatches()
                    FuzzySubstringMatchingStrategy.FUZZY_PREFIX ->
                        wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
                    else ->
                        wasMatchingBefore
                }
    }

    private fun getErrorStrategies(nextNode: TrieNode<T>): List<SearchWithErrorStrategy<T>> {
        return listOf(
            // 1. misspelling: increment searchIndex and go to the next node
            SearchWithErrorStrategy(
                nextNode,
                searchCoordinates.searchIndex + 1,
                StringBuilder(searchVariables.sequence).append(nextNode.string)
            ),
            // 2. missing letter in data: increment searchIndex and stay at the previous node
            SearchWithErrorStrategy(
                searchVariables.node,
                searchCoordinates.searchIndex + 1,
                StringBuilder(searchVariables.sequence)
            ),
            // 3. missing letter in search keyword: keep searchIndex the same and go to the next node
            SearchWithErrorStrategy(
                nextNode,
                searchCoordinates.searchIndex,
                StringBuilder(searchVariables.sequence).append(nextNode.string)
            )
        )
    }

    private fun buildResetState(
        nextNode: TrieNode<T>,
        forceReturn: Boolean = false
    ): Collection<FuzzySubstringSearchState<T>>? {

        return when {
            !forceReturn && (searchVariables.isGatherState || matches()) ->
                null
            else ->
                listOf(
                    FuzzySubstringSearchState(
                        searchRequest = searchRequest,
                        searchVariables = SearchVariables(
                            node = nextNode,
                            sequence = StringBuilder(searchVariables.sequence).append(nextNode.string),
                            isGatherState = false
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
    }

    private fun buildGatherState(nextNode: TrieNode<T>): List<FuzzySubstringSearchState<T>> {
        val gatherStates = mutableListOf<FuzzySubstringSearchState<T>>()

        val defaultGatherState = FuzzySubstringSearchState(
            searchRequest = searchRequest,
            searchVariables = SearchVariables(
                node = nextNode,
                sequence = StringBuilder(searchVariables.sequence).append(nextNode.string),
                isGatherState = true,
            ),
            searchCoordinates = searchCoordinates
        )

        gatherStates.add(defaultGatherState)

        // we spin off a new reset state, in case we find a better match further in the string.
        // we must only do this once, if this is the first time this state enters into the gather state
        val perfectMatch = searchRequest.search.length == searchCoordinates.numberOfMatches
        if (!searchVariables.isGatherState && !perfectMatch) {
            gatherStates.addAll(buildResetState(nextNode, true)!!)
        }

        return gatherStates
    }

    private fun searchCoordinatesMatch(nextNode: TrieNode<T>): Boolean {
        return searchCoordinates.searchIndex < searchRequest.search.length
                && nextNode.string == searchRequest.search[searchCoordinates.searchIndex].toString()
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
        val distanceToWordSeparator = searchVariables.sequence.length - indexOfLastWordSeparator - 2
        return distanceToWordSeparator <= searchCoordinates.numberOfErrors
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
        return this == "" /* == root */ || this.matches(wordSeparatorRegex)
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
                    isGatherState = false,
                ),
                searchCoordinates = SearchCoordinates(0, 0, 0, null, null)
            )
        }
    }
}

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
    val isGatherState: Boolean,
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