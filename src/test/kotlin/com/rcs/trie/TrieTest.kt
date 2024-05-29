package com.rcs.trie

import org.assertj.core.api.Assertions.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.Collection
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
    fun testMatchBySubstring() {
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
        val resultA: Collection<TrieSearchResult<Int>> =
            trie.matchBySubstring("a") // match a prefix of length 1

        val resultB: Collection<TrieSearchResult<Int>> =
            trie.matchBySubstring("def") // match a prefix of length > 1

        val resultC: Collection<TrieSearchResult<Int>> =
            trie.matchBySubstring("ghi") // match a postfix & substring

        val resultD: Collection<TrieSearchResult<Int>> =
            trie.matchBySubstring("jklmno") // match the whole sequence

        val resultE: Collection<TrieSearchResult<Int>> =
            trie.matchBySubstring("pqs") // match after an initial failed attempt

        val resultF: Collection<TrieSearchResult<Int>> =
            trie.matchBySubstring("vw") // matched whole word

        val resultG: Collection<TrieSearchResult<Int>> =
            trie.matchBySubstring("234") // only partial match

        // Assert
        assertThat(resultA).containsExactlyInAnyOrder(
            TrieSearchResult("abcdef", 1, "a", "abcdef", 1, 0, 0, false, false)
        )

        assertThat(resultB).containsExactlyInAnyOrder(
            TrieSearchResult("abcdef", 1, "def", "abcdef", 3, 0, 3, false, false),
            TrieSearchResult("defghi", 2, "def", "defghi", 3, 0, 0, false, false)
        )

        assertThat(resultC).containsExactlyInAnyOrder(
            TrieSearchResult("defghi", 2, "ghi", "defghi", 3, 0, 3, false, false),
            TrieSearchResult("deghij", 3, "ghi", "deghij", 3, 0, 2, false, false)
        )

        assertThat(resultD).containsExactlyInAnyOrder(
            TrieSearchResult("jklmno", 4, "jklmno", "jklmno", 6, 0, 0, true, true)
        )

        assertThat(resultE).containsExactlyInAnyOrder(
            TrieSearchResult("pqrpqs", 5, "pqs", "pqrpqs", 3, 0, 3, false, false)
        )

        assertThat(resultF).containsExactlyInAnyOrder(
            TrieSearchResult("tu vw, xyz", 6, "vw", "vw", 2, 0, 0, false, true)
        )

        assertThat(resultG).isEmpty()
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