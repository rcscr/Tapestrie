package com.rcs.trie

class Trie<T> {

    private lateinit var root: TrieNode<T>

    init {
        clear()
    }

    fun clear() {
        root = TrieNode("", null, mutableSetOf())
    }

    /**
     * returns the previous value, if any, associated with this key
     */
    fun put(input: String, value: T): T? {
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
                    val valueToInsert = if (reachedEndOfInput) value else null
                    val next = TrieNode(currentCharacter, valueToInsert, mutableSetOf())
                    current.next.add(next)
                    current = next

                // we are at the last character of the input
                // we have a string going this far, so we modify it, setting it to complete
                // (if its already complete, that means we have already inserted the same input before)
                // see TrieTest.testAddShorterAfter
                } else if (reachedEndOfInput) {
                    val completed = TrieNode(nextMatchingNode.string, value, nextMatchingNode.next)
                    current.next.removeIf { it.string == nextMatchingNode.string }
                    current.next.add(completed)
                    return nextMatchingNode.value

                // there is a matching node, but we're not at the end of the input yet,
                // so go on to the next character
                } else {
                    current = nextMatchingNode
                }
            }
        }
        return null
    }

    fun remove(input: String): T? {
        var current = root

        val deque = ArrayDeque<TrieNode<T>>(input.length + 1)
        deque.add(root)

        for (element in input) {
            val currentCharacter = element.toString()

            var nextMatchingNode: TrieNode<T>?
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
            var nodeToUnlink: TrieNode<T>
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
        return FuzzySubstringSearcher.search(root, search, errorTolerance, matchingStrategy)
    }

    private fun prefixMatchUpTo(string: String): TrieNode<T>? {
        var current = root

        for (element in string) {
            val currentCharacter = element.toString()

            var nextSubstring: TrieNode<T>? = null
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

    private fun gatherAll(start: TrieNode<T>, startSequence: String): MutableMap<String, T> {
        val results = mutableMapOf<String, T>()
        val queue = ArrayDeque<Pair<TrieNode<T>, String>>()
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

    private fun TrieNode<T>.isUsedForOtherStrings(): Boolean {
        return this === root || this.completes() || this.next.size > 1
    }
}