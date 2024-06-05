package com.rcs.trie

class TrieIterator<T>(val root: TrieNode<T>): Iterator<TrieEntry<T>> {

    val queue = ArrayDeque<Pair<TrieNode<T>, String>>()
    var next: TrieEntry<T>? = null

    init {
        queue.add(Pair(root, ""))
        setNext()
    }

    override fun hasNext(): Boolean {
        return next != null
    }

    override fun next(): TrieEntry<T> {
        val toReturn = next
        setNext()
        return toReturn!!
    }

    private fun setNext() {
        // breadth-first search: returns strings from shortest to longest
        while(queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val node = current.first
            val sequence = current.second
            for (next in node.next) {
                queue.add(Pair(next, sequence + next.string))
            }
            if (node.completes()) {
                next = TrieEntry(sequence, node.value!!)
                return
            }
        }
        next = null
    }
}