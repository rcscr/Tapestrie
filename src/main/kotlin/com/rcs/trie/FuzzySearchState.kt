package com.rcs.trie

import com.rcs.trie.FuzzyMatchingStrategy.*
import com.rcs.trie.FuzzySearchUtils.Companion.compare
import com.rcs.trie.FuzzySearchUtils.Companion.indexOfFirstWordSeparator
import com.rcs.trie.FuzzySearchUtils.Companion.indexOfLastWordSeparator
import com.rcs.trie.FuzzySearchUtils.Companion.isWordSeparator
import com.rcs.trie.FuzzySearchUtils.Companion.isWordSeparatorAt

/**
 * Invariable properties of the search request - these never change.
 */
private data class SearchRequest(
    val matchingStrategy: FuzzyMatchingStrategy,
    val matchingOptions: MatchingOptions,
    val keyword: String,
    val numberOfPredeterminedErrors: Int,
    val errorTolerance: Int,
)

/**
 * Variable properties that change depending on what TrieNode we're looking at.
 */
private data class SearchVariables<T>(
    val node: TrieNode<T>,
    val sequence: String,
    val isGatherState: Boolean,
)

/**
 * The search coordinates, which may or may not lead to a successful match.
 */
private data class SearchCoordinates(
    val keywordIndex: Int,
    val numberOfMatches: Int,
    val numberOfErrors: Int,
    val numberOfCaseMismatches: Int,
    val numberOfDiacriticMismatches: Int,
    val startMatchIndex: Int?,
    val endMatchIndex: Int?,
    val swapChars: List<SwapChars>?
)

/**
 * Holds swap characters for matching SWAP strategies
 */
private data class SwapChars(
    val fromSource: String,
    val fromTarget: String
)

/**
 * A convenience class for passing around new search error states.
 */
private data class ErrorStrategy<T>(
    val node: TrieNode<T>,
    val searchIndex: Int,
    val sequence: String,
    val swapChar: SwapChars?,
    val startMatchIndex: Int?
)

class FuzzySearchState<T> private constructor(
    private val searchRequest: SearchRequest,
    private val searchVariables: SearchVariables<T>,
    private val searchCoordinates: SearchCoordinates
) {

    fun nextStates(): Collection<FuzzySearchState<T>> {
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
        if (!hasSearchResult()) {
            throw IllegalStateException("State does not have a search result")
        }

        val numberOfErrors = getNumberOfErrorsIncludingMissingCharacters() +
                searchRequest.numberOfPredeterminedErrors

        val assertedStartMatchIndex = searchCoordinates.startMatchIndex!!

        val assertedEndMatchIndex = searchCoordinates.endMatchIndex!!

        val matchedWholeSequence = numberOfErrors == 0
                && matchedWholeSequence(assertedStartMatchIndex, assertedEndMatchIndex)

        val matchedWholeWord = numberOfErrors == 0
                && matchedWholeWord(assertedStartMatchIndex, assertedEndMatchIndex)

        val indexOfWordSeparatorBefore = searchVariables.sequence
            .indexOfLastWordSeparator(assertedStartMatchIndex) ?: -1

        val indexOfWordSeparatorAfter = searchVariables.sequence
            .indexOfFirstWordSeparator(assertedEndMatchIndex + 1) ?: searchVariables.sequence.length

        val prefixDistance = assertedStartMatchIndex - indexOfWordSeparatorBefore - 1

        val matchedSubstringEndIndex = when(searchRequest.matchingStrategy) {
            ACRONYM ->
                searchVariables.sequence.indexOfFirstWordSeparator(assertedEndMatchIndex + 1)
                    ?: searchVariables.sequence.length
            else ->
                assertedEndMatchIndex + 1
        }

        val matchedSubstring = searchVariables.sequence.substring(
            assertedStartMatchIndex, matchedSubstringEndIndex)

        val matchedWord = searchVariables.sequence.substring(
            indexOfWordSeparatorBefore + 1, indexOfWordSeparatorAfter)

        return TrieSearchResult(
            string = searchVariables.sequence,
            value = searchVariables.node.value!!,
            matchedSubstring = matchedSubstring,
            matchedWord = matchedWord,
            numberOfMatches = searchCoordinates.numberOfMatches,
            numberOfErrors = numberOfErrors,
            numberOfCaseMismatches = searchCoordinates.numberOfCaseMismatches,
            numberOfDiacriticMismatches = searchCoordinates.numberOfDiacriticMismatches,
            prefixDistance = prefixDistance,
            matchedWholeString = matchedWholeSequence,
            matchedWholeWord = matchedWholeWord,
        )
    }

    private fun matches(): Boolean {
        return searchCoordinates.startMatchIndex != null
                && searchCoordinates.endMatchIndex != null
                && hasMinimumNumberOfMatches()
                && getNumberOfErrorsIncludingMissingCharacters() <= searchRequest.errorTolerance
                && searchCoordinates.swapChars?.isEmpty() ?: true
    }

    private fun hasMinimumNumberOfMatches(): Boolean {
        val minimumRequiredMatches = searchRequest.keyword.length - searchRequest.errorTolerance
        return searchCoordinates.numberOfMatches >= minimumRequiredMatches
    }

    private fun nextStates(nextNode: TrieNode<T>): Collection<FuzzySearchState<T>>? {
        if (shouldCull(nextNode)) {
            return null
        }

        return buildContinueState(nextNode)
            ?: buildMatchState(nextNode)
            ?: buildErrorState(nextNode)
            ?: buildResetState(nextNode)
            ?: buildGatherState(nextNode)
    }

    private fun shouldCull(nextNode: TrieNode<T>): Boolean {
        val numberOfMatchingCharactersNeeded = searchRequest.keyword.length -
                searchCoordinates.numberOfMatches -
                searchRequest.errorTolerance

        return nextNode.depth < numberOfMatchingCharactersNeeded
    }

    private fun buildContinueState(nextNode: TrieNode<T>): Collection<FuzzySearchState<T>>? {
        return if (searchRequest.matchingStrategy == ACRONYM
            && !searchVariables.node.string.isWordSeparator()) {

            listOf(
                FuzzySearchState(
                    searchRequest,
                    SearchVariables(
                        node = nextNode,
                        sequence = searchVariables.sequence + nextNode.string,
                        isGatherState = false
                    ),
                    searchCoordinates
                )
            )
        } else {
            null
        }
    }

    private fun buildMatchState(nextNode: TrieNode<T>): Collection<FuzzySearchState<T>>? {
        if (searchVariables.isGatherState) {
            return null
        }

        return nextNodeMatches(nextNode)
            ?.let {
                val newCaseMismatch = if (!it.exactMatch && it.caseInsensitiveMatch == true) 1 else 0
                val newDiacriticMismatch = if (!it.exactMatch && it.diacriticInsensitiveMatch == true) 1 else 0
                listOf(
                    FuzzySearchState(
                        searchRequest,
                        SearchVariables(
                            node = nextNode,
                            sequence = searchVariables.sequence + nextNode.string,
                            isGatherState = false
                        ),
                        SearchCoordinates(
                            startMatchIndex = searchCoordinates.startMatchIndex ?: searchVariables.sequence.length,
                            endMatchIndex = searchVariables.sequence.length,
                            keywordIndex = searchCoordinates.keywordIndex + 1,
                            numberOfMatches = searchCoordinates.numberOfMatches + 1,
                            numberOfErrors = searchCoordinates.numberOfErrors,
                            numberOfCaseMismatches = searchCoordinates.numberOfCaseMismatches + newCaseMismatch,
                            numberOfDiacriticMismatches = searchCoordinates.numberOfDiacriticMismatches + newDiacriticMismatch,
                            swapChars = searchCoordinates.swapChars
                        )
                    )
                )
            }
    }

    private fun nextNodeMatches(nextNode: TrieNode<T>): TrieNodeMatchResult? {
        if (searchRequest.matchingStrategy == WILDCARD && currentSearchCharacter() == "*") {
            // todo: create a field called `wildcardMatch`in TrieNodeMatchResult
            return TrieNodeMatchResult(exactMatch = true)
        }

        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0

        val matchingPreconditions = when (searchRequest.matchingStrategy) {
            ACRONYM ->
                searchVariables.node.string.isWordSeparator()
            FUZZY_PREFIX ->
                wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
            EXACT_PREFIX, FUZZY_POSTFIX ->
                wasMatchingBefore || searchVariables.node.string.isWordSeparator()
            else ->
                true
        }

        val searchOutOfBounds = searchCoordinates.keywordIndex >= searchRequest.keyword.length

        if (!matchingPreconditions || searchOutOfBounds) {
            return null
        }

        return nextNode.string
            .compare(currentSearchCharacter(), searchRequest.matchingOptions)
            .let {
                if (it.anyMatch) it
                else null
            }
    }

    private fun buildErrorState(nextNode: TrieNode<T>): Collection<FuzzySearchState<T>>? {
        if (searchVariables.isGatherState) {
            return null
        }

        val swapSatisfied = searchCoordinates.swapChars.getMatching(nextNode)

        return when {
            swapSatisfied != null -> {
                listOf(
                    FuzzySearchState(
                        searchRequest,
                        SearchVariables(
                            node = nextNode,
                            sequence = searchVariables.sequence + nextNode.string,
                            isGatherState = false
                        ),
                        SearchCoordinates(
                            startMatchIndex = searchCoordinates.startMatchIndex ?: searchVariables.sequence.length,
                            endMatchIndex = searchVariables.sequence.length,
                            keywordIndex = searchCoordinates.keywordIndex + 1,
                            numberOfMatches = searchCoordinates.numberOfMatches,
                            numberOfErrors = searchCoordinates.numberOfErrors + 1,
                            numberOfCaseMismatches = searchCoordinates.numberOfCaseMismatches,
                            numberOfDiacriticMismatches = searchCoordinates.numberOfDiacriticMismatches,
                            swapChars = searchCoordinates.swapChars!!.filter { it != swapSatisfied }
                        )
                    )
                )
            }
            shouldProduceErrorStates() ->
                getErrorStrategies(nextNode).map {
                    FuzzySearchState(
                        searchRequest,
                        SearchVariables(
                            node = it.node,
                            sequence = it.sequence,
                            isGatherState = false
                        ),
                        SearchCoordinates(
                            startMatchIndex = it.startMatchIndex,
                            endMatchIndex = searchCoordinates.endMatchIndex,
                            keywordIndex = it.searchIndex,
                            numberOfMatches = searchCoordinates.numberOfMatches,
                            numberOfErrors = searchCoordinates.numberOfErrors + 1,
                            numberOfCaseMismatches = searchCoordinates.numberOfCaseMismatches,
                            numberOfDiacriticMismatches = searchCoordinates.numberOfDiacriticMismatches,
                            swapChars = it.swapChar?.let { swapChar ->
                                (searchCoordinates.swapChars ?: mutableListOf()) + swapChar
                            }
                        )
                    )
                }
            else ->
                null
        }
    }

    private fun shouldProduceErrorStates(): Boolean {
        val hasNoPendingSwaps = searchCoordinates.swapChars?.isEmpty() ?: true
        val wasMatchingBefore = searchCoordinates.numberOfMatches > 0
        val hasSearchCharacters = searchCoordinates.keywordIndex + 1 < searchRequest.keyword.length
        val hasErrorAllowance = searchCoordinates.numberOfErrors < searchRequest.errorTolerance

        val errorPreconditions = when (searchRequest.matchingStrategy) {
            SYMMETRICAL_SWAP ->
                true
            ADJACENT_SWAP ->
                hasNoPendingSwaps
            FUZZY_POSTFIX ->
                hasMinimumNumberOfMatches()
            FUZZY_PREFIX ->
                wasMatchingBefore || distanceToStartWordSeparatorIsPermissible()
            else ->
                wasMatchingBefore
        }

        return errorPreconditions
                && hasSearchCharacters
                && hasErrorAllowance
    }

    private fun getErrorStrategies(nextNode: TrieNode<T>): List<ErrorStrategy<T>> {
        // 1. Swap: increment searchIndex and go to the next node, and keep track of swap letters
        if (searchRequest.matchingStrategy == ADJACENT_SWAP || searchRequest.matchingStrategy == SYMMETRICAL_SWAP) {
            val typoSwapStrategy = ErrorStrategy(
                nextNode,
                searchCoordinates.keywordIndex + 1,
                searchVariables.sequence + nextNode.string,
                SwapChars(currentSearchCharacter(), nextNode.string),
                searchCoordinates.startMatchIndex ?: searchVariables.sequence.length
            )
            return listOf(typoSwapStrategy)
        }

        // 2. misspelling: increment searchIndex and go to the next node
        val misspellingStrategy = ErrorStrategy(
            nextNode,
            searchCoordinates.keywordIndex + 1,
            searchVariables.sequence + nextNode.string,
            null,
            searchCoordinates.startMatchIndex
        )

        // 3. missing letter in data: increment searchIndex and stay at the previous node
        val missingTargetLetterStrategy = ErrorStrategy(
            searchVariables.node,
            searchCoordinates.keywordIndex + 1,
            searchVariables.sequence,
            null,
            searchCoordinates.startMatchIndex
        )

        // 4. missing letter in search keyword: keep searchIndex the same and go to the next node
        val missingSourceLetterStrategy = ErrorStrategy(
            nextNode,
            searchCoordinates.keywordIndex,
            searchVariables.sequence + nextNode.string,
            null,
            searchCoordinates.startMatchIndex
        )

        return listOf(misspellingStrategy, missingTargetLetterStrategy, missingSourceLetterStrategy)
    }

    private fun buildResetState(
        nextNode: TrieNode<T>,
        forceReturn: Boolean = false
    ): Collection<FuzzySearchState<T>>? {

        return when {
            forceReturn || (!searchVariables.isGatherState && !matches()) ->
                listOf(
                    FuzzySearchState(
                        searchRequest,
                        SearchVariables(
                            node = nextNode,
                            sequence = searchVariables.sequence + nextNode.string,
                            isGatherState = false
                        ),
                        searchCoordinates = SearchCoordinates(
                            startMatchIndex = null,
                            endMatchIndex = null,
                            keywordIndex = 0,
                            numberOfMatches = 0,
                            numberOfErrors = 0,
                            numberOfCaseMismatches = 0,
                            numberOfDiacriticMismatches = 0,
                            swapChars = null
                        )
                    )
                )
            else ->
                null
        }
    }

    private fun buildGatherState(nextNode: TrieNode<T>): List<FuzzySearchState<T>> {
        val gatherStates = mutableListOf<FuzzySearchState<T>>()

        val defaultGatherState = FuzzySearchState(
            searchRequest,
            SearchVariables(
                node = nextNode,
                sequence = searchVariables.sequence + nextNode.string,
                isGatherState = true,
            ),
            searchCoordinates
        )

        gatherStates.add(defaultGatherState)

        // we spin off a new reset state, in case we find a better match further in the string.
        // we must only do this once: if this is the first time this state enters into the gather state.
        val notPerfectMatch = searchRequest.keyword.length != searchCoordinates.numberOfMatches
        if (!searchVariables.isGatherState && notPerfectMatch) {
            gatherStates.addAll(buildResetState(nextNode, true)!!)
        }

        return gatherStates
    }

    private fun currentSearchCharacter(): String {
        return searchRequest.keyword[searchCoordinates.keywordIndex].toString()
    }

    private fun getNumberOfErrorsIncludingMissingCharacters(): Int {
        val unmatchedCharacters = searchRequest.keyword.length - searchCoordinates.keywordIndex
        if (unmatchedCharacters < 0) {
            throw AssertionError("Number of unmatched characters should never be negative")
        }
        return searchCoordinates.numberOfErrors +
                unmatchedCharacters
    }

    private fun distanceToStartWordSeparatorIsPermissible(): Boolean {
        val indexOfLastWordSeparator = searchVariables.sequence.indexOfLastWordSeparator() ?: -1
        val distanceToWordSeparator = searchVariables.sequence.length - indexOfLastWordSeparator - 2
        return distanceToWordSeparator <= searchCoordinates.numberOfErrors
    }

    private fun matchedWholeSequence(startMatchIndex: Int, endMatchIndex: Int): Boolean {
        return startMatchIndex == 0 && endMatchIndex == searchVariables.sequence.length - 1
    }

    private fun matchedWholeWord(startMatchIndex: Int, endMatchIndex: Int): Boolean {
        return searchVariables.sequence.isWordSeparatorAt(startMatchIndex - 1)
                && searchVariables.sequence.isWordSeparatorAt(endMatchIndex + 1)
    }

    private fun List<SwapChars>?.getMatching(nextNode: TrieNode<T>): SwapChars? {
        return this?.firstOrNull {
            it.fromSource.compare(nextNode.string, searchRequest.matchingOptions).anyMatch
                    && it.fromTarget.compare(currentSearchCharacter(), searchRequest.matchingOptions).anyMatch
        }
    }

    companion object {

        fun <T> getInitialStates(
            root: TrieNode<T>,
            keyword: String,
            errorTolerance: Int,
            matchingStrategy: FuzzyMatchingStrategy,
            matchingOptions: MatchingOptions,
        ): Collection<FuzzySearchState<T>> {

            val initialStates = mutableListOf<FuzzySearchState<T>>()

            val defaultInitialState = FuzzySearchState(
                root, keyword, 0, errorTolerance, matchingStrategy, matchingOptions)

            initialStates.add(defaultInitialState)

            // efficient way to match with errors in beginning
            if (matchingStrategy == LIBERAL) {
                for (i in 1..errorTolerance) {
                    val stateWithPredeterminedError = FuzzySearchState(
                        root,
                        keyword.substring(i, keyword.length),
                        numberOfPredeterminedErrors = i,
                        errorTolerance - i,
                        matchingStrategy,
                        matchingOptions,
                    )
                    initialStates.add(stateWithPredeterminedError)
                }
            }

            return initialStates
        }


        /**
         * `invoke` emulates a public constructor
         */
        private operator fun <T> invoke(
            root: TrieNode<T>,
            keyword: String,
            numberOfPredeterminedErrors: Int,
            errorTolerance: Int,
            matchingStrategy: FuzzyMatchingStrategy,
            matchingOptions: MatchingOptions,
        ): FuzzySearchState<T> {

            // this class only work works when beginning with the root node
            if (!root.isRoot()) {
                throw IllegalArgumentException("Node must be root")
            }

            return FuzzySearchState(
                SearchRequest(
                    matchingStrategy,
                    matchingOptions,
                    keyword,
                    numberOfPredeterminedErrors,
                    errorTolerance,
                ),
                SearchVariables(
                    node = root,
                    sequence = "",
                    isGatherState = false,
                ),
                SearchCoordinates(
                    keywordIndex = 0,
                    numberOfMatches= 0,
                    numberOfErrors = 0,
                    numberOfCaseMismatches = 0,
                    numberOfDiacriticMismatches = 0,
                    startMatchIndex = null,
                    endMatchIndex = null,
                    swapChars = null
                )
            )
        }
    }
}