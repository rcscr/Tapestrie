package com.rcs.trie

class Trie<T> {

    private lateinit var root: Node<T>

    init {
        clear()
    }

    fun clear() {
        root = Node("", null, mutableSetOf())
    }

    fun put(input: String, value: T) {
        if (input.isEmpty()) {
            throw IllegalArgumentException("Cannot add an empty string")
        }

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
        val last = deque.removeLast()

        if (last.completes()) {
            // look back until you first the last character that is not used for other strings
            var j = input.length - 1
            var nodeToUnlink: Node<T>
            while (!deque.removeLast().also { nodeToUnlink = it }.isUsedForOtherStrings()) {
                j--
            }
            // unlink this last character, thus completing the removal
            val charToUnlink = input[j].toString()
            synchronized(nodeToUnlink.next) {
                nodeToUnlink.next.removeIf { it.string == charToUnlink }
            }
            return last.value
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
            gatherAll(it, prefix)
        } ?: mutableMapOf()
    }

    fun matchBySubstring(search: String): List<TrieSearchResult<T>> {
        return matchBySubstringFuzzy(search, 0, FuzzySubstringMatchingStrategy.LIBERAL)
    }

    fun matchBySubstringFuzzy(
        search: String,
        errorTolerance: Int,
        matchingStrategy: FuzzySubstringMatchingStrategy
    ): List<TrieSearchResult<T>> {

        if (search.isEmpty() || errorTolerance < 0 || errorTolerance > search.length) {
            throw IllegalArgumentException()
        }

        val results = mutableMapOf<String, TrieSearchResult<T>>()

        val queue = ArrayDeque<FuzzySubstringSearchState<T>>()
        val initialState = FuzzySubstringSearchState(search, root, null, null, 0, 0, 0, errorTolerance, StringBuilder())
        queue.add(initialState)

        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()

            if (state.matches()) {
                val newMatches = gatherAll(state)
                results.putOnlyNewOrBetterMatches(newMatches)
                continue
            }

            var nextNodes: Array<Node<T>>
            synchronized(state.node.next) {
                nextNodes = state.node.next.toTypedArray()
            }
            for (nextNode in nextNodes) {
                queue.addAll(state.nextSearchStates(nextNode, matchingStrategy))
            }
        }

        return results.values.sortedWith(TrieSearchResultComparator.sortByBestMatchFirst)
    }

    private fun gatherAll(initialState: FuzzySubstringSearchState<T>): MutableMap<String, TrieSearchResult<T>> {
        val results = mutableMapOf<String, TrieSearchResult<T>>()
        val queue = ArrayDeque<FuzzySubstringSearchState<T>>()
        queue.add(initialState)

        while(queue.isNotEmpty()) {
            val state = queue.removeFirst()

            if (state.node.completes()) {
                val searchResult = state.buildSearchResult()
                results[searchResult.string] = searchResult
            }

            var nextNodes: Array<Node<T>>
            synchronized(state.node.next) {
                nextNodes = state.node.next.toTypedArray()
            }
            for (nextNode in nextNodes) {
                queue.add(state.nextBuildState(nextNode))
            }
        }

        return results
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

    private fun gatherAll(start: Node<T>, startSequence: String): MutableMap<String, T> {
        val results = mutableMapOf<String, T>()
        val queue = ArrayDeque<Pair<Node<T>, String>>()
        queue.add(Pair(start, startSequence))

        while(queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val node = current.first
            val sequence = current.second
            if (node.completes()) {
                results[sequence] = node.value!!
            }
            for (next in node.next) {
                queue.add(Pair(next, sequence + next.string))
            }
        }

        return results
    }

    private fun Node<T>.isUsedForOtherStrings(): Boolean {
        return this === root || this.completes() || this.next.size > 1
    }

    private fun <T> MutableMap<String, TrieSearchResult<T>>
            .putOnlyNewOrBetterMatches(newMatches: MutableMap<String, TrieSearchResult<T>>) {
        newMatches.entries
            .filter {
                this[it.key] == null
                        || this[it.key]!!.lengthOfMatch < it.value.lengthOfMatch
                        || this[it.key]!!.errors > it.value.errors
            }
            .forEach { this[it.key] = it.value }
    }
}