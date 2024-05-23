package com.rcs.trie

class Node<T>(val string: String, val value: T?, val next: MutableSet<Node<T>>) {
    fun completes(): Boolean {
        return value != null
    }
}