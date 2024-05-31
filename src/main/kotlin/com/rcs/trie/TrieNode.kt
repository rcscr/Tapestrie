package com.rcs.trie

data class TrieNode<T>(val string: String, var value: T?, val next: MutableSet<TrieNode<T>>) {
    fun completes(): Boolean {
        return value != null
    }
}