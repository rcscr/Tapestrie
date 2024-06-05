package com.rcs.trie

class TrieIterator<T>(val root: TrieNode<T>): Iterator<Pair<String, T>> {

    val queue = ArrayDeque<Pair<TrieNode<T>, String>>()
    var next: Pair<String, T>? = null

    init {
        queue.add(Pair(root, ""))
        setNext()
    }

    override fun hasNext(): Boolean {
        return next != null
    }

    override fun next(): Pair<String, T> {
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
                next = Pair(sequence, node.value!!)
                return
            }
        }
        next = null
    }
}