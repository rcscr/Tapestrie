package com.rcs.trie

import org.assertj.core.api.Assertions.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors
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

    @Test
    fun testMatchByPrefix() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("Hello, Nomads!", 1)
        trie.put("Hello, World!", 2)
        trie.put("Hi there!", 3)

        // Act
        val matchedHello = trie.matchByPrefix("Hello")
        val matchedHi = trie.matchByPrefix("Hi")
        val matchedH = trie.matchByPrefix("H")
        val matchedBlank = trie.matchByPrefix("")
        val matchFail = trie.matchByPrefix("O")

        // Assert
        assertThat(matchedHello).isEqualTo(
            mapOf(
                "Hello, Nomads!" to 1,
                "Hello, World!" to 2
            )
        )

        assertThat(matchedHi).isEqualTo(
            mapOf(
                "Hi there!" to 3
            )
        )

        assertThat(matchedH).isEqualTo(
            mapOf(
                "Hello, Nomads!" to 1,
                "Hello, World!" to 2,
                "Hi there!" to 3
            )
        )

        assertThat(matchedBlank).isEqualTo(
            mapOf(
                "Hello, Nomads!" to 1,
                "Hello, World!" to 2,
                "Hi there!" to 3
            )
        )

        assertThat(matchFail).isEmpty()
    }

    @Test
    fun testConcurrency() {
        // Arrange
        val trie = Trie<Int>()
        val executorService = Executors.newFixedThreadPool(8)
        val randomStrings = (0..10_000).map { getRandomString() }.distinct()

        // Act
        randomStrings
            .map { executorService.submit { trie.put(it, 123) } }
            .forEach { it.get() }

        // Assert
        assertThat(trie.matchByPrefix("").size).isEqualTo(randomStrings.size)

        // parallel match and remove
        randomStrings
            .map {
                executorService.submit {
                    val substring = it.substring(0, 10)
                    val matched = trie.matchByPrefix(substring)
                    matched.keys.forEach {
                        assertThat(trie.remove(it)).isNotNull()
                        assertThat(trie.getExactly(it)).isNull()
                    }
                }
            }
            .forEach { it.get() }

        assertThat(trie.matchByPrefix("").size).isEqualTo(0)
    }

    private fun getRandomString(): String {
        val array = ByteArray(20)
        Random().nextBytes(array)
        return String(array, StandardCharsets.UTF_8)
    }
}