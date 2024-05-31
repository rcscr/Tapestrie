package com.rcs.trie

import org.assertj.core.api.Assertions.*
import kotlin.test.Test

class TrieTest {

    @Test
    fun testClear() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("Hello", 123)
        assertThat(trie.getExactly("Hello")).isEqualTo(123)

        // Act
        trie.clear()

        // Assert
        assertThat(trie.matchByPrefix("")).isEmpty()
    }

    @Test
    fun testAdd() {
        // Arrange
        val trie = Trie<Int>()

        // Act
        trie.put("Hello, Nomads!", 123)

        // Assert
        assertThat(trie.containsExactly("Hello")).isFalse()
        assertThat(trie.containsExactly("Hello, World!")).isFalse()
        assertThat(trie.containsExactly("Hello, Nomads!")).isTrue()
    }

    @Test
    fun testUpdate() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("Hello", 123)

        // Act
        val previousValue = trie.put("Hello", 456)

        // Assert
        assertThat(previousValue).isEqualTo(123)
        assertThat(trie.getExactly("Hello")).isEqualTo(456)
    }

    @Test
    fun testAddShorterAfter() {
        // Arrange
        val trie = Trie<Int>()

        // Act
        trie.put("123456", 1)
        trie.put("12345", 2)
        trie.put("1234", 3)

        // Assert
        assertThat(trie.containsExactly("123456")).isTrue()
        assertThat(trie.containsExactly("12345")).isTrue()
        assertThat(trie.containsExactly("1234")).isTrue()
    }

    @Test
    fun testRemove() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("Hello, Nomads!", 1)
        trie.put("Hello, World!", 1)

        // Act
        val result = trie.remove("Hello, Nomads!")

        // Assert
        assertThat(result).isEqualTo(1)
        assertThat(trie.containsExactly("Hello, Nomads!")).isFalse()
        assertThat(trie.containsExactly("Hello, World!")).isTrue()
    }

    @Test
    fun testRemoveNonExistent() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("Hello, Nomads!", 1)
        trie.put("Hello, World!", 1)

        // Act
        val result = trie.remove("Hello, People!")

        // Assert
        assertThat(result).isNull()
        assertThat(trie.containsExactly("Hello, Nomads!")).isTrue()
        assertThat(trie.containsExactly("Hello, World!")).isTrue()
    }
}