package com.rcs.trie

import kotlin.math.max

class Trie<T>: Iterable<TrieEntry<T>> {

    private val depthUpdateLock = Any()

    private lateinit var root: TrieNode<T>

    init {
        clear()
    }

    fun clear() {
        root = TrieNode("", null, 0, mutableSetOf(), null)
    }

    fun depth(): Int {
        return root.depth
    }

    fun isEmpty(): Boolean {
        return root.next.isEmpty()
    }

    override fun iterator(): Iterator<TrieEntry<T>> {
        return TrieIterator(root)
    }

    /**
     * Returns the previous value, if any, associated with this key (inputString)
     */
    fun put(inputString: String, value: T): T? {
        if (inputString.isEmpty()) {
            throw IllegalArgumentException("Cannot add an empty string")
        }

        var previousValue: T? = null

        synchronized(depthUpdateLock) {
            var current = root

            for (i in inputString.indices) {
                val reachedEndOfInput = i == inputString.length - 1

                val currentCharacter = inputString[i].toString()

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
                        val depth = inputString.length - i - 1
                        val previous = current
                        val nextNode = TrieNode(currentCharacter, valueToInsert, depth, mutableSetOf(), previous)
                        current.next.add(nextNode)
                        current = nextNode

                    // we are at the last character of the input
                    // we have a string going this far, so we modify it, setting it to complete
                    // (if its already complete, that means we have already inserted the same input before)
                    // see TrieBasicTest.testAddShorterAfter
                    } else if (reachedEndOfInput) {
                        previousValue = nextMatchingNode.value
                        nextMatchingNode.value = value

                    // there is a matching node, but we're not at the end of the input yet,
                    // so go on to the next character
                    } else {
                        current = nextMatchingNode
                    }
                }
            }

            updateDepths(current, current.next.maxByOrNull { it.depth })
        }

        return previousValue
    }

    /**
     * Returns the previous value, if any, associated with this key (inputString)
     */
    fun remove(inputString: String): T? {
        synchronized(depthUpdateLock) {
            val deque = ArrayDeque<TrieNode<T>>(inputString.length + 1)
            deque.add(root)

            var current = root

            for (element in inputString) {
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
                // look back until we find the first character that is used for other strings
                var j = inputString.length - 1
                var nodeFromWhichToUnlink: TrieNode<T>
                while (!deque.removeLast().also { nodeFromWhichToUnlink = it }.isUsedForOtherStrings()) {
                    j--
                }

                // unlink this last character and update sizes
                synchronized(nodeFromWhichToUnlink.next) {
                    val charToUnlink = inputString[j].toString()
                    nodeFromWhichToUnlink.next.removeIf { it.string == charToUnlink }
                    updateDepths(nodeFromWhichToUnlink, nodeFromWhichToUnlink.next.maxByOrNull { it.depth })
                }

                return last.value
            }
        }

        return null
    }

    fun getExactly(string: String): T? {
        return prefixMatchUpTo(string)?.value
    }

    fun containsExactly(string: String): Boolean {
        return getExactly(string) != null
    }

    fun matchByPrefix(prefix: String): Map<String, T> {
        return prefixMatchUpTo(prefix)
            ?.let { gatherAll(it, prefix) }
            ?: mutableMapOf()
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
        return TrieIterator(start)
            .asSequence()
            .fold(mutableMapOf()) { map, next ->
                map[startSequence + next.string] = next.value
                map
            }
    }

    private fun updateDepths(current: TrieNode<T>?, next: TrieNode<T>?) {
        current?.let {
            val maxDepth = current.next.filter { it != next }.maxOfOrNull { it.depth } ?: 0
            current.depth = max(next?.depth ?: 0, maxDepth) + current.string.length
            updateDepths(current.previous, current)
        }
    }

    private fun TrieNode<T>.isUsedForOtherStrings(): Boolean {
        return this === root || this.completes() || this.next.size > 1
    }
}