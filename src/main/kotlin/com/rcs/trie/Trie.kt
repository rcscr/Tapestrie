package com.rcs.trie

class Trie<T> {

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

    fun remove(input: String) {
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
                return // input does not exist
            } else {
                current = nextMatchingNode!!
                deque.add(current)
            }
        }

        var last = deque.removeLast()

        // if it does not complete, input does not exist
        if (last.completes()) {
            var j = input.length - 1
            while (!deque.removeLast().also { last = it }.isUsedForOtherStrings()) {
                j--
            }
            val charToUnlink = input[j].toString()
            synchronized(last.next) {
                last.next.removeIf { it.string == charToUnlink }
            }
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
            root,
            null,
            consecutiveMatches =  0,
            errorTolerance,
            errorsEncountered = 0,
            StringBuilder(),
            matches
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
        current: Node<T>,
        leftOfFirstMatchingCharacter: Node<T>?,
        consecutiveMatches: Int,
        errorTolerance: Int,
        errorsEncountered: Int,
        sequence: StringBuilder,
        accumulation: MutableCollection<TrieSearchResult<T>>
    ) {
        val match = consecutiveMatches == search.length && errorsEncountered <= errorTolerance
        val partialMatch = current.completes() && consecutiveMatches >= search.length - errorTolerance

        if (match || partialMatch) {
            findCompleteStringsStartingAt(
                current,
                leftOfFirstMatchingCharacter!!, // if matched, this is automatically not null
                null,
                search,
                consecutiveMatches,
                errorsEncountered,
                sequence,
                accumulation,
                mutableMapOf()
            )
            return
        }

        var nextNodes: MutableSet<Node<T>>
        synchronized(current.next) {
            nextNodes = current.next.toMutableSet()
        }

        for (nextNode in nextNodes) {
            val currentNodeMatches = consecutiveMatches > 0
            val nextNodeMatches = nextNode.string == search[consecutiveMatches].toString()

            var newConsecutiveMatches: Int
            var newErrorsEncountered: Int
            if (nextNodeMatches) {
                newConsecutiveMatches = consecutiveMatches + 1
                newErrorsEncountered = errorsEncountered
            } else if (currentNodeMatches && errorsEncountered < errorTolerance) {
                // was matching before, but no longer matches; however, there's some error tolerance to be used
                newConsecutiveMatches = consecutiveMatches + 1
                newErrorsEncountered = errorsEncountered + 1
            } else {
                // reset
                newConsecutiveMatches = 0
                newErrorsEncountered = 0
            }

            val newLeftOfFirstMatchingCharacter =
                if (!currentNodeMatches && nextNodeMatches) current
                else leftOfFirstMatchingCharacter

            // looking ahead allows examining potential matches with letters missing
            // I could use better nomenclature for this flow, but it does work
            val shouldLookAhead = !nextNodeMatches
                    && errorsEncountered < errorTolerance
                    && consecutiveMatches + 1 < search.length
                    && nextNode.string == search[consecutiveMatches + 1].toString()

            val attempts = if (shouldLookAhead) 2 else 1

            for (i in 0..<attempts) {
                findCompleteStringsBySubstring(
                    search,
                    nextNode,
                    newLeftOfFirstMatchingCharacter,
                    newConsecutiveMatches + i,
                    errorTolerance,
                    newErrorsEncountered,
                    StringBuilder(sequence).append(nextNode.string),
                    accumulation
                )
            }
        }
    }

    private fun findCompleteStringsStartingAt(
        current: Node<T>,
        leftOfFirstMatchingCharacter: Node<T>,
        rightOfLastMatchingCharacter: Node<T>?,
        search: String,
        consecutiveMatches: Int,
        errors: Int,
        matchUpToHere: StringBuilder,
        accumulation: MutableCollection<TrieSearchResult<T>>,
        alreadySaved: MutableMap<String, Int>
    ) {
        if (current.completes()) {
            val matchUpToHereString = matchUpToHere.toString()

            val lengthOfMatch = consecutiveMatches - errors

            val hasAlreadySaved = alreadySaved[matchUpToHereString] != null
            val foundBetterMatch = !hasAlreadySaved || lengthOfMatch < alreadySaved[matchUpToHereString]!!

            if (!hasAlreadySaved || foundBetterMatch) {
                val actualErrors =
                    if (matchUpToHereString.length < search.length) search.length - lengthOfMatch
                    else errors

                val matchedWholeSequence = actualErrors == 0
                        && matchedWholeSequence(leftOfFirstMatchingCharacter, rightOfLastMatchingCharacter)

                val matchedWholeWord = actualErrors == 0
                        && matchedWholeWord(leftOfFirstMatchingCharacter, rightOfLastMatchingCharacter)

                val newSearchResult = TrieSearchResult<T>(
                    matchUpToHereString,
                    current.value!!,
                    lengthOfMatch,
                    actualErrors,
                    matchedWholeSequence,
                    matchedWholeWord
                )

                accumulation.removeIf { it.string == matchUpToHereString }
                accumulation.add(newSearchResult)
                alreadySaved[matchUpToHereString] = lengthOfMatch
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
                errors,
                StringBuilder(matchUpToHere).append(nextNode.string),
                accumulation,
                alreadySaved
            )
        }
    }

    private fun matchedWholeSequence(
        leftOfFirstMatchingCharacter: Node<T>?,
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

    private fun findCompleteStringsStartingAt(
        current: Node<T>,
        sequence: String,
        accumulation: MutableMap<String, T>
    ) {
        if (current.completes()) {
            accumulation[sequence] = current.value!!
        }
        for (next in current.next) {
            findCompleteStringsStartingAt(next, sequence + next.string, accumulation)
        }
    }

    private fun Node<T>?.isWordSeparator(): Boolean {
        return this == null || this == root || this.string.matches(wholeWordSeparator.toRegex()) ?: false
    }

    private fun Node<T>.isUsedForOtherStrings(): Boolean {
        return this === root || this.completes() || this.next.size > 1
    }
}