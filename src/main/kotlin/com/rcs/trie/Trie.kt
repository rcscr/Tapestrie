package com.rcs.trie

class Trie<T> {

    private val wholeWordSeparator = "[^a-zA-Z\\d:]"

    private val sortByLengthOfMatchLongestFirst: Comparator<SearchResult<T>> =
        compareBy(SearchResult<T>::lengthOfMatch).reversed()

    private val sortByLengthOfStringShortestFirst: Comparator<SearchResult<T>> =
        compareBy { it.string.length }

    private val sortByMatchedSequenceTrueFirst: Comparator<SearchResult<T>> =
        compareBy(SearchResult<T>::matchedWholeWord).reversed()

    private val sortByMatchedWholeWordTrueFirst: Comparator<SearchResult<T>> =
        compareBy(SearchResult<T>::matchedWholeWord).reversed()

    private val sortByBestMatchFirst: Comparator<SearchResult<T>> =
        sortByLengthOfMatchLongestFirst
            .thenComparing(sortByMatchedSequenceTrueFirst)
            .thenComparing(sortByMatchedWholeWordTrueFirst)
            .thenComparing(sortByLengthOfStringShortestFirst)

    data class SearchResult<T>(
        val string: String,
        val value: T,
        val lengthOfMatch: Int,
        val matchedWholeSequence: Boolean,
        val matchedWholeWord: Boolean
    )

    class Node<T>(val string: String, val value: T?, val next: MutableSet<Node<T>>) {
        fun completes(): Boolean {
            return value != null
        }
    }

    private lateinit var root: Node<T>

    init {
        clear()
    }

    fun clear() {
        root = Node<T>("", null, mutableSetOf())
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

    fun remove(input: String) {
        var current = root

        val deque = ArrayDeque<Node<T>>(input.length + 1)
        deque.add(root)

        for (element in input) {
            val currentCharacter = element.toString()

            var nextMatchingNode: Node<T>?

            synchronized(current.next) {
                nextMatchingNode = current.next
                    .firstOrNull { it.string == currentCharacter }
            }

            // input does not exist
            if (nextMatchingNode == null) {
                return
            } else {
                current = nextMatchingNode!!
                deque.add(current)
            }
        }

        var last = deque.removeLast()

        // if it does not complete, input does not exist
        if (last.completes()) {
            var j = input.length - 1
            while (!isUsedForOtherStrings(deque.removeLast().also { last = it })) {
                j--
            }
            val charToUnlink = input[j].toString()
            synchronized(last.next) {
                last.next.removeIf { it.string == charToUnlink }
            }
        }
    }

    fun matchByPrefix(prefix: String): Map<String, T> {
        return prefixMatchUpTo(prefix)?.let {
            val matches: MutableMap<String, T> = HashMap()
            findCompleteStringsStartingAt(it, prefix, matches)
            matches
        } ?: mutableMapOf()
    }

    fun matchByExactSubstring(search: String): List<SearchResult<T>> {
        return matchBySubstring(search, search.length)
    }

    fun matchBySubstring(search: String, minLength: Int): List<SearchResult<T>> {
        if (search.isEmpty() || minLength < 1 || minLength > search.length) {
            throw IllegalArgumentException()
        }

        val matches: MutableList<SearchResult<T>> = mutableListOf()
        val alreadySaved: MutableSet<String> = HashSet()

        findCompleteStringsBySubstring(
            root,
            null,
            search,
            0,
            minLength,
            StringBuilder(),
            matches,
            alreadySaved
        )

        return matches.sortedWith(sortByBestMatchFirst)
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
        current: Node<T>,
        leftOfFirstMatchingCharacter: Node<T>?,
        search: String,
        consecutiveMatches: Int,
        minLength: Int,
        matchUpToHere: StringBuilder,
        accumulation: MutableCollection<SearchResult<T>>,
        alreadySaved: MutableSet<String>
    ) {
        if (consecutiveMatches == minLength) {
            findCompleteStringsStartingAt(
                current,
                leftOfFirstMatchingCharacter,
                null,
                search,
                consecutiveMatches,
                matchUpToHere,
                accumulation,
                alreadySaved
            )
            return
        }

        val thisNodeMatches = consecutiveMatches > 0

        var nextNodes: Set<Node<T>>

        synchronized(current.next) {
            nextNodes = current.next.toMutableSet()
        }

        for (nextNode in nextNodes) {
            val nextNodeMatches = nextNode.string == search[consecutiveMatches].toString()

            findCompleteStringsBySubstring(
                nextNode,
                if (!thisNodeMatches && nextNodeMatches) current else leftOfFirstMatchingCharacter,
                search,
                if (nextNodeMatches) consecutiveMatches + 1 else 0,
                minLength,
                StringBuilder(matchUpToHere).append(nextNode.string),
                accumulation,
                alreadySaved
            )
        }
    }

    private fun findCompleteStringsStartingAt(
        current: Node<T>,
        leftOfFirstMatchingCharacter: Node<T>?,
        rightOfLastMatchingCharacter: Node<T>?,
        search: String,
        consecutiveMatches: Int,
        matchUpToHere: StringBuilder,
        accumulation: MutableCollection<SearchResult<T>>,
        alreadySaved: MutableSet<String>
    ) {
        if (current.completes()) {
            val matchUpToHereString = matchUpToHere.toString()

            if (!alreadySaved.contains(matchUpToHereString)) {
                val matchedWholeSequence = getMatchedWholeSequence(
                    leftOfFirstMatchingCharacter, rightOfLastMatchingCharacter)
                val matchedWholeWord = getMatchedWholeWord(
                    leftOfFirstMatchingCharacter, rightOfLastMatchingCharacter)

                val newSearchResult = SearchResult<T>(
                    matchUpToHereString,
                    current.value!!,
                    consecutiveMatches,
                    matchedWholeSequence,
                    matchedWholeWord
                )

                accumulation.add(newSearchResult)
                alreadySaved.add(matchUpToHereString)
            }
        }

        var nextNodes: Set<Node<T>>

        synchronized(current.next) {
            nextNodes = current.next.toMutableSet()
        }

        val endMatch = null != rightOfLastMatchingCharacter

        for (nextNode in nextNodes) {
            val nextNodeMatches = !endMatch && consecutiveMatches < search.length
                    && nextNode.string == search[consecutiveMatches].toString()

            val nextRightOfLastMatchingCharacter =
                if (!endMatch && !nextNodeMatches) nextNode
                else rightOfLastMatchingCharacter

            val newConsecutiveMatches =
                if (!endMatch && nextNodeMatches) consecutiveMatches + 1
                else consecutiveMatches

            findCompleteStringsStartingAt(
                nextNode,
                leftOfFirstMatchingCharacter,
                nextRightOfLastMatchingCharacter,
                search,
                newConsecutiveMatches,
                StringBuilder(matchUpToHere).append(nextNode.string),
                accumulation,
                alreadySaved
            )
        }
    }

    private fun isWordSeparator(node: Node<T>?): Boolean {
        return node?.string?.matches(wholeWordSeparator.toRegex()) ?: false
    }

    private fun getMatchedWholeSequence(
        leftOfFirstMatchingCharacter: Node<T>?,
        rightOfLastMatchingCharacter: Node<T>?
    ): Boolean {
        return leftOfFirstMatchingCharacter == root && rightOfLastMatchingCharacter == null
    }

    private fun getMatchedWholeWord(
        leftOfFirstMatchingCharacter: Node<T>?,
        rightOfLastMatchingCharacter: Node<T>?
    ): Boolean {
        return (leftOfFirstMatchingCharacter == root || isWordSeparator(leftOfFirstMatchingCharacter))
                &&
                (rightOfLastMatchingCharacter == null || isWordSeparator(rightOfLastMatchingCharacter))
    }

    private fun findCompleteStringsStartingAt(
        current: Node<T>,
        matchUpToHere: String,
        accumulation: MutableMap<String, T>
    ) {
        if (current.completes()) {
            accumulation[matchUpToHere] = current.value!!
        }
        for (next in current.next) {
            findCompleteStringsStartingAt(next, matchUpToHere + next.string, accumulation)
        }
    }

    private fun isUsedForOtherStrings(node: Node<T>): Boolean {
        return node === root || node.completes() || node.next.size > 1
    }
}