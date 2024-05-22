package com.rcs.trie

import org.assertj.core.api.Assertions
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
        Assertions.assertThat(trie.matchByPrefix("")).isEqualTo(mapOf("Hello" to 123))

        // Act
        trie.clear()

        // Assert
        Assertions.assertThat(trie.matchByPrefix("")).isEmpty()
    }

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
            trie.matchByExactSubstring("jklmno") // match the whole sequence

        val resultE: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("pqs") // match after an initial failed attempt

        val resultF: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("vw") // matched whole word

        val resultG: Collection<Trie.SearchResult<Int>> =
            trie.matchByExactSubstring("234") // only partial match

        // Assert
        Assertions.assertThat(resultA).containsExactlyInAnyOrder(
            Trie.SearchResult("abcdef", 1, 1, false, false)
        )

        Assertions.assertThat(resultB).containsExactlyInAnyOrder(
            Trie.SearchResult("abcdef", 1, 3, false, false),
            Trie.SearchResult("defghi", 2, 3, false, false)
        )

        Assertions.assertThat(resultC).containsExactlyInAnyOrder(
            Trie.SearchResult("defghi", 2, 3, false, false),
            Trie.SearchResult("deghij", 3, 3, false, false)
        )

        Assertions.assertThat(resultD).containsExactlyInAnyOrder(
            Trie.SearchResult("jklmno", 4, 6, true, true)
        )

        Assertions.assertThat(resultE).containsExactlyInAnyOrder(
            Trie.SearchResult("pqrpqs", 5, 3, false, false)
        )

        Assertions.assertThat(resultF).containsExactlyInAnyOrder(
            Trie.SearchResult("tu vw, xyz", 6, 2, false, true)
        )

        Assertions.assertThat(resultG).isEmpty()
    }

    @Test
    fun testMatchBySubstringWithMinLength() {
        // Arrange
        val trie = Trie<Int>()
        trie.put("google", 1)
        trie.put("googlo", 2)
        trie.put("googly", 3)
        trie.put("googu", 4)

        // Act
        val result = trie.matchBySubstring("googly", 5)

        // Assert
        Assertions.assertThat(result).containsExactly(
            Trie.SearchResult("googly", 3, 6, true, true),
            Trie.SearchResult("google", 1, 5, false, false),
            Trie.SearchResult("googlo", 2, 5, false, false),
        )
    }

    @Test
    fun testMatchBySubstringWithSort() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("man", Unit)
        trie.put("many", Unit)
        trie.put("manual", Unit)
        trie.put("manually", Unit)
        trie.put("manuals", Unit)
        trie.put("linux manual", Unit)

        // Act
        val result = trie.matchBySubstring("manual", 3)

        // Assert
        Assertions.assertThat(result).containsExactly(
            // matches whole sequence is highest ranking
            Trie.SearchResult("manual", Unit, 6, true, true),
            // matches a whole word
            Trie.SearchResult("linux manual", Unit, 6, false, true),
            // matches the highest possible number of characters, but it's neither the whole sequence nor a whole word
            Trie.SearchResult("manuals", Unit, 6, false, false),
            // same as above, but the string is longer, so is ranked lower
            Trie.SearchResult("manually", Unit, 6, false, false),
            // partial match, but matched whole sequence and whole word
            Trie.SearchResult("man", Unit, 3, true, true),
            // partial match, but string longer, so ranked lower
            Trie.SearchResult("many", Unit, 3, false, false),
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