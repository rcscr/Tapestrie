package com.rcs.trie

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class TrieIteratorTest {

    @Test
    fun testIterator() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("Hey", 0)
        trie.put("Oi", 1)
        trie.put("Coucou", 2)
        trie.put("Hallo", 3)
        trie.put("Konnichiwa", 4)
        trie.put("Hujambo", 5)

        // Act
        val iterated = mutableListOf<Pair<String, Int>>()

        for (pair in trie) {
            iterated.add(pair)
        }

        // Assert
        assertThat(iterated).containsExactly(
            Pair("Oi", 1),
            Pair("Hey", 0),
            Pair("Hallo", 3),
            Pair("Coucou", 2),
            Pair("Hujambo", 5),
            Pair("Konnichiwa", 4),
        )
    }
}