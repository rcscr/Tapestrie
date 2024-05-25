package com.rcs.trie

class Trie<T> {

    private data class FuzzySubstringSearchState<T>(
        val node: Node<T>,
        val leftOfFirstMatchingCharacter: Node<T>?,
        val rightOfLastMatchingCharacter: Node<T>?,
        val searchIndex: Int,
        val numberOfMatches: Int,
        val numberOfErrors: Int,
        val sequence: StringBuilder,
    )

    private val wholeWordSeparator = "[\\s\\p{P}]"

    private lateinit var root: Node<T>

    init {
        clear()
    }

    fun clear() {
        root = Node("", null, mutableSetOf())
    }

    fun put(input: String, value: T) {
        var current = root

        for (i in input.indices) {
            val reachedEndOfInput = i == input.length - 1

            val currentCharacter = input[i].toString()

            synchronized(current.next) {
                val nextMatchingNode = current.next
                    .stream()
                    .filter { it.string == currentCharacter }
                    .findAny()
                    .orElse(null)

                // we do not have a string going this far, so we create a new node,
                // and then keep appending the remaining characters of the input to it
                if (null == nextMatchingNode) {
                    val next = Node(currentCharacter, if (reachedEndOfInput) value else null, mutableSetOf())
                    current.next.add(next)
                    current = next

                // we are at the last character of the input
                // we have a string going this far, so we modify it, setting it to complete
                // (if its already complete, that means we have already inserted the same input before)
                // see TrieTest.testAddShorterAfter
                } else if (reachedEndOfInput && nextMatchingNode.value == null) {
                    val completed = Node(nextMatchingNode.string, value, nextMatchingNode.next)
                    current.next.removeIf { it.string == nextMatchingNode.string }
                    current.next.add(completed)

                // there is a matching node, but we're not at the end of the input yet,
                // so go on to the next character
                } else {
                    current = nextMatchingNode
                }
            }
        }
    }

    fun remove(input: String): T? {
        var current = root

        val deque = ArrayDeque<Node<T>>(input.length + 1)
        deque.add(root)

        for (element in input) {
            val currentCharacter = element.toString()

            var nextMatchingNode: Node<T>?
            synchronized(current.next) {
                nextMatchingNode = current.next.firstOrNull { it.string == currentCharacter }
            }

            if (nextMatchingNode == null) {
                return null // input does not exist
            } else {
                current = nextMatchingNode!!
                deque.add(current)
            }
        }

        // this is the node to remove - but only if it completes
        var last = deque.removeLast()

        if (last.completes()) {
            // remove all characters, unless they're used for other strings
            val value = last.value
            var j = input.length - 1
            while (!deque.removeLast().also { last = it }.isUsedForOtherStrings()) {
                j--
            }
            val charToUnlink = input[j].toString()
            synchronized(last.next) {
                last.next.removeIf { it.string == charToUnlink }
            }
            return value
        } else {
            return null // if it does not complete, input does not exist
        }
    }

    fun getExactly(string: String): T? {
        return prefixMatchUpTo(string)?.let {
            if (it.completes()) {
                it.value
            } else {
                null
            }
        }
    }

    fun containsExactly(string: String): Boolean {
        return getExactly(string) != null
    }

    fun matchByPrefix(prefix: String): Map<String, T> {
        return prefixMatchUpTo(prefix)?.let {
            val matches: MutableMap<String, T> = HashMap()
            findCompleteStringsStartingAt(it, prefix, matches)
            matches
        } ?: mutableMapOf()
    }

    fun matchBySubstring(search: String): List<TrieSearchResult<T>> {
        return matchBySubstringFuzzy(search, 0)
    }

    fun matchBySubstringFuzzy(search: String, errorTolerance: Int): List<TrieSearchResult<T>> {
        if (search.isEmpty() || errorTolerance < 0 || errorTolerance > search.length) {
            throw IllegalArgumentException()
        }

        val matches: MutableList<TrieSearchResult<T>> = mutableListOf()

        findCompleteStringsBySubstring(
            search,
            matches,
            errorTolerance,
            FuzzySubstringSearchState(root, null, null, 0, 0, 0, StringBuilder())
        )

        return matches.sortedWith(TrieSearchResultComparator.sortByBestMatchFirst)
    }

    private fun prefixMatchUpTo(string: String): Node<T>? {
        var current = root

        for (element in string) {
            val currentCharacter = element.toString()

            var nextSubstring: Node<T>? = null

            synchronized(current.next) {
                nextSubstring = current.next.firstOrNull { it.string == currentCharacter }
            }

            if (nextSubstring == null) {
                return null
            } else {
                current = nextSubstring!!
            }
        }

        return current
    }

    private fun findCompleteStringsBySubstring(
        search: String,
        results: MutableCollection<TrieSearchResult<T>>,
        errorTolerance: Int,
        state: FuzzySubstringSearchState<T>,
    ) {
        val match = state.searchIndex >= search.length
                && state.numberOfMatches >= search.length - state.numberOfErrors
                && state.numberOfErrors <= errorTolerance

        val partialMatch = state.node.completes()
                && state.numberOfMatches >= search.length - errorTolerance

        if (match || partialMatch) {
            findCompleteStringsStartingAt(search, results, state)
            return
        }

        var nextNodes: MutableSet<Node<T>>
        synchronized(state.node.next) {
            nextNodes = state.node.next.toMutableSet()
        }

        val currentNodeMatches = state.numberOfMatches > 0

        for (nextNode in nextNodes) {
            val nextNodeMatches = nextNode.string == search[state.searchIndex].toString()

            val searchStates = mutableListOf<FuzzySubstringSearchState<T>>()

            if (nextNodeMatches) {
                // happy path - continue matching
                searchStates.add(FuzzySubstringSearchState(
                    node = nextNode,
                    leftOfFirstMatchingCharacter = state.leftOfFirstMatchingCharacter ?: state.node,
                    rightOfLastMatchingCharacter = null,
                    searchIndex = state.searchIndex + 1,
                    numberOfMatches = state.numberOfMatches + 1,
                    numberOfErrors = state.numberOfErrors,
                    sequence = StringBuilder(state.sequence).append(nextNode.string)
                ))
            } else if (currentNodeMatches && state.numberOfErrors < errorTolerance) {
                // was matching before, but no longer matches;
                // however, there's some error tolerance to be used
                // there are three ways this can go: misspelling, missing letter in search input, or missing letter in data

                // misspelling
                // increment searchIndex and go to the next node
                if (state.searchIndex + 1 < search.length) {
                    searchStates.add(FuzzySubstringSearchState(
                        node = nextNode,
                        leftOfFirstMatchingCharacter = state.leftOfFirstMatchingCharacter,
                        rightOfLastMatchingCharacter = null,
                        searchIndex = state.searchIndex + 1,
                        numberOfMatches = state.numberOfMatches + 1,
                        numberOfErrors = state.numberOfErrors + 1,
                        sequence = StringBuilder(state.sequence).append(nextNode.string)
                    ))
                }

                // missing letter in data
                // increment searchIndex and go to the previous node
                if (state.searchIndex + 1 < search.length) {
                    searchStates.add(FuzzySubstringSearchState(
                        node = state.node,
                        leftOfFirstMatchingCharacter = state.leftOfFirstMatchingCharacter,
                        rightOfLastMatchingCharacter = null,
                        searchIndex = state.searchIndex + 1,
                        numberOfMatches = state.numberOfMatches + 1,
                        numberOfErrors = state.numberOfErrors + 1,
                        sequence = StringBuilder(state.sequence)
                    ))
                }

                // missing letter in search input
                // keep searchIndex the same and go to the next node
                searchStates.add(FuzzySubstringSearchState(
                    node = nextNode,
                    leftOfFirstMatchingCharacter = state.leftOfFirstMatchingCharacter,
                    rightOfLastMatchingCharacter = null,
                    searchIndex = state.searchIndex ,
                    numberOfMatches = state.numberOfMatches + 1,
                    numberOfErrors = state.numberOfErrors + 1,
                    sequence = StringBuilder(state.sequence).append(nextNode.string)
                ))
            } else {
                // reset matching
                searchStates.add(FuzzySubstringSearchState(
                    node = nextNode,
                    leftOfFirstMatchingCharacter = null,
                    rightOfLastMatchingCharacter = null,
                    searchIndex = 0,
                    numberOfMatches = 0,
                    numberOfErrors = 0,
                    sequence = StringBuilder(state.sequence).append(nextNode.string)
                ))
            }

            searchStates.forEach {
                findCompleteStringsBySubstring(
                    search,
                    results,
                    errorTolerance,
                    it
                )
            }
        }
    }

    private fun findCompleteStringsStartingAt(
        search: String,
        results: MutableCollection<TrieSearchResult<T>>,
        state: FuzzySubstringSearchState<T>,
    ) {
        if (state.node.completes()) {
            val sequenceString = state.sequence.toString()

            val lengthOfMatch = state.numberOfMatches - state.numberOfErrors
            val actualErrors =
                if (sequenceString.length < search.length) search.length - lengthOfMatch
                else state.numberOfErrors

            val existing = results.find { it.string == sequenceString }
            val isBetterMatch = existing == null || existing.lengthOfMatch < lengthOfMatch

            if (isBetterMatch) {
                val matchedWholeSequence = actualErrors == 0
                        && matchedWholeSequence(state.leftOfFirstMatchingCharacter!!, state.rightOfLastMatchingCharacter)

                val matchedWholeWord = actualErrors == 0
                        && matchedWholeWord(state.leftOfFirstMatchingCharacter!!, state.rightOfLastMatchingCharacter)

                val newSearchResult = TrieSearchResult<T>(
                    sequenceString,
                    state.node.value!!,
                    lengthOfMatch,
                    actualErrors,
                    matchedWholeSequence,
                    matchedWholeWord
                )
                existing?.let { results.remove(it) }
                results.add(newSearchResult)
            }
        }

        var nextNodes: Set<Node<T>>
        synchronized(state.node.next) {
            nextNodes = state.node.next.toMutableSet()
        }

        val endMatch = null != state.rightOfLastMatchingCharacter

        for (nextNode in nextNodes) {
            val nextNodeMatches = !endMatch && state.numberOfMatches < search.length
                    && nextNode.string == search[state.numberOfMatches].toString()

            val nextRightOfLastMatchingCharacter =
                if (!endMatch && !nextNodeMatches) nextNode
                else state.rightOfLastMatchingCharacter

            val newConsecutiveMatches =
                if (!endMatch && nextNodeMatches) state.numberOfMatches + 1
                else state.numberOfMatches

            findCompleteStringsStartingAt(
                search,
                results,
                FuzzySubstringSearchState(
                    node = nextNode,
                    leftOfFirstMatchingCharacter = state.leftOfFirstMatchingCharacter,
                    rightOfLastMatchingCharacter = nextRightOfLastMatchingCharacter,
                    searchIndex = state.searchIndex,
                    numberOfMatches = newConsecutiveMatches,
                    numberOfErrors = state.numberOfErrors,
                    sequence = StringBuilder(state.sequence).append(nextNode.string)
                )
            )
        }
    }

    private fun matchedWholeSequence(
        leftOfFirstMatchingCharacter: Node<T>,
        rightOfLastMatchingCharacter: Node<T>?
    ): Boolean {
        return leftOfFirstMatchingCharacter == root
                && rightOfLastMatchingCharacter == null
    }

    private fun matchedWholeWord(
        leftOfFirstMatchingCharacter: Node<T>,
        rightOfLastMatchingCharacter: Node<T>?
    ): Boolean {
        return leftOfFirstMatchingCharacter.isWordSeparator()
                && rightOfLastMatchingCharacter.isWordSeparator()
    }

    private fun findCompleteStringsStartingAt(current: Node<T>, sequence: String, results: MutableMap<String, T>) {
        if (current.completes()) {
            results[sequence] = current.value!!
        }
        for (next in current.next) {
            findCompleteStringsStartingAt(next, sequence + next.string, results)
        }
    }

    private fun Node<T>?.isWordSeparator(): Boolean {
        return this == null || this == root || this.string.matches(wholeWordSeparator.toRegex()) ?: false
    }

    private fun Node<T>.isUsedForOtherStrings(): Boolean {
        return this === root || this.completes() || this.next.size > 1
    }
}