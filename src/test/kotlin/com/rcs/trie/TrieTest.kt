package com.rcs.trie

import org.assertj.core.api.Assertions
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlin.collections.Collection
import kotlin.test.Test

class TrieTest {

    @Test
    fun testAdd() {
        // Arrange
        val trie = Trie<Int>()

        // Act
        trie.put("Hello, Nomads!", 123)

        // Assert
        Assertions.assertThat(trie.containsExactly("Hello")).isFalse()
        Assertions.assertThat(trie.containsExactly("Hello, World!")).isFalse()
        Assertions.assertThat(trie.containsExactly("Hello, Nomads!")).isTrue()
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
        Assertions.assertThat(trie.containsExactly("123456")).isTrue()
        Assertions.assertThat(trie.containsExactly("12345")).isTrue()
        Assertions.assertThat(trie.containsExactly("1234")).isTrue()
    }

    @Test
    fun testRemove() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("Hello, Nomads!", 1)
        trie.put("Hello, World!", 1)

        // Act
        trie.remove("Hello, Nomads!")

        // Assert
        Assertions.assertThat(trie.matchByPrefix("Hello, Talk")).isEmpty()
        Assertions.assertThat(trie.containsExactly("Hello, Nomads!")).isFalse()
        Assertions.assertThat(trie.containsExactly("Hello, World!")).isTrue()
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

        // Assert
        Assertions.assertThat(matchedHello).isEqualTo(
            mapOf(
                "Hello, Nomads!" to 1,
                "Hello, World!" to 2
            )
        )

        Assertions.assertThat(matchedHi).isEqualTo(
            mapOf(
                "Hi there!" to 3
            )
        )

        Assertions.assertThat(matchedH).isEqualTo(
            mapOf(
                "Hello, Nomads!" to 1,
                "Hello, World!" to 2,
                "Hi there!" to 3
            )
        )

        Assertions.assertThat(matchedBlank).isEqualTo(
            mapOf(
                "Hello, Nomads!" to 1,
                "Hello, World!" to 2,
                "Hi there!" to 3
            )
        )
    }

    @Test
    fun testMatchByExactSubstring() {
        // Arrange
        val trie = Trie<Int>()

        trie.put("abcdef", 1)
        trie.put("defghi", 2)
        trie.put("deghij", 3)
        trie.put("jklmno", 4)
        trie.put("pqrpqs", 5)
        trie.put("tu vw, xyz", 6)
        trie.put("123", 7)

        // Act
        val resultA: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("a") // match a prefix of length 1

        val resultB: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("def") // match a prefix of length > 1

        val resultC: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("ghi") // match a postfix & substring

        val resultD: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("jklmno") // match an entire string

        val resultE: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("pqs") // match after an initial failed attempt

        val resultF: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("vw") // matched whole word

        val resultG: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("234") // only partial match

        // Assert
        Assertions.assertThat(resultA).containsExactlyInAnyOrder(
            Trie.SearchResult("abcdef", 1, 1, false)
        )

        Assertions.assertThat(resultB).containsExactlyInAnyOrder(
            Trie.SearchResult("abcdef", 1, 3, false),
            Trie.SearchResult("defghi", 2, 3, false)
        )

        Assertions.assertThat(resultC).containsExactlyInAnyOrder(
            Trie.SearchResult("defghi", 2, 3, false),
            Trie.SearchResult("deghij", 3, 3, false)
        )

        Assertions.assertThat(resultD).containsExactlyInAnyOrder(
            Trie.SearchResult("jklmno", 4, 6, true)
        )

        Assertions.assertThat(resultE).containsExactlyInAnyOrder(
            Trie.SearchResult("pqrpqs", 5, 3, false)
        )

        Assertions.assertThat(resultF).containsExactlyInAnyOrder(
            Trie.SearchResult("tu vw, xyz", 6, 2, true)
        )

        Assertions.assertThat(resultG).isEmpty()
    }

    @Test
    fun testMatchByExactSubstringWithLength() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("google", 1)

        // Act
        val result = trie.matchBySubstring("googly", 5)

        // Assert
        Assertions.assertThat(result).containsExactly(
            Trie.SearchResult("google", 1, 5, false)
        )
    }

    @Test
    fun testMatchByExactSubstringWithLengthWholeWord() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("googl", 1)

        // Act
        val result = trie.matchBySubstring("google", 5)

        // Assert
        Assertions.assertThat(result).containsExactly(
            Trie.SearchResult("googl", 1, 5, true)
        )
    }

    @Test
    fun testConcurrency() {
        // Arrange
        val trie = Trie<Int>()
        val executorService = Executors.newFixedThreadPool(8)
        val randomStrings = (0..1000).map { getRandomString() }.distinct()

        // Act
        randomStrings
            .map { executorService.submit { trie.put(it, 123) } }
            .forEach { it.get() }

        // Assert
        Assertions.assertThat(trie.matchByPrefix("").size).isEqualTo(randomStrings.size)

        // parallel match and remove
        randomStrings
            .map {
                executorService.submit {
                    val substring = it.substring(0, 10)
                    val matched = trie.matchByPrefix(substring)
                    matched.keys.forEach { trie.remove(it) }
                }
            }
            .forEach { it.get() }

        Assertions.assertThat(trie.matchByPrefix("").size).isEqualTo(0)
    }

    private fun getRandomString(): String {
        val array = ByteArray(20)
        Random().nextBytes(array)
        return String(array, StandardCharsets.UTF_8)
    }
}