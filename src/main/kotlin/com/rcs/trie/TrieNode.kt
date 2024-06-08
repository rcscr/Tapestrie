package com.rcs.trie

class TrieNode<T>(
    val string: String,
    var value: T?,
    var depth: Int,
    val next: MutableSet<TrieNode<T>>,
    val previous: TrieNode<T>?
) {

    fun completes(): Boolean {
        return value != null
    }

    fun getNextNode(string: String): TrieNode<T>? {
        return next.firstOrNull { it.string == string }
    }
}