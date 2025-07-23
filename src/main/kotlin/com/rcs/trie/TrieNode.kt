package com.rcs.trie

import com.rcs.trie.Utils.Companion.removeDiacritics

class TrieNode<T> private constructor(
    val string: String,
    var value: T?,
    var depth: Int,
    val next: MutableSet<TrieNode<T>>,
    val previous: TrieNode<T>?,
    val lowercaseString: String?,
    val withoutDiacriticsString: String?,
    val lowercaseAndWithoutDiacriticsString: String?,
) {

    companion object {

        fun <T> newRoot(): TrieNode<T> {
            return TrieNode("", null, 0, mutableSetOf(), null, null, null, null)
        }

        /**
         * `invoke` emulates a public constructor
         * encapsulates creation of normalized versions of the string
         */
        operator fun <T> invoke(
            string: String,
            value: T?,
            depth: Int,
            next: MutableSet<TrieNode<T>>,
            previous: TrieNode<T>?
        ): TrieNode<T> {
            val lowercase = string.lowercase()
            val withoutDiacritics = string.removeDiacritics()
            val lowercaseAndWithoutDiacriticsString = string.lowercase().removeDiacritics()

            return TrieNode(
                string,
                value,
                depth,
                next,
                previous,
                lowercaseString =
                    lowercase.takeIf { it != string },
                withoutDiacriticsString =
                    withoutDiacritics.takeIf { it != string },
                lowercaseAndWithoutDiacriticsString =
                    lowercaseAndWithoutDiacriticsString.takeIf { it != string },
            )
        }
    }

    fun isRoot(): Boolean {
        return string == "" && previous == null
    }

    fun completes(): Boolean {
        return value != null
    }

    fun getNextNode(string: String): TrieNode<T>? {
        synchronized(next) {
            return next.firstOrNull { it.string == string }
        }
    }

    fun addNextNode(node: TrieNode<T>) {
        synchronized(next) {
            next.add(node)
        }
    }

    fun removeNextNode(string: String) {
        synchronized(next) {
            next.removeIf { it.string == string }
        }
    }
}