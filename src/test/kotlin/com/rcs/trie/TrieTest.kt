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
        Assertions.assertThat(trie.getExactly("Hello")).isEqualTo(123)

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
        Assertions.assertThat(resultA).containsExactlyInAnyOrder(
            TrieSearchResult("abcdef", 1, 1, 0, false, false)
        )

        Assertions.assertThat(resultB).containsExactlyInAnyOrder(
            TrieSearchResult("abcdef", 1, 3, 0, false, false),
            TrieSearchResult("defghi", 2, 3, 0, false, false)
        )

        Assertions.assertThat(resultC).containsExactlyInAnyOrder(
            TrieSearchResult("defghi", 2, 3, 0, false, false),
            TrieSearchResult("deghij", 3, 3, 0, false, false)
        )

        Assertions.assertThat(resultD).containsExactlyInAnyOrder(
            TrieSearchResult("jklmno", 4, 6, 0, true, true)
        )

        Assertions.assertThat(resultE).containsExactlyInAnyOrder(
            TrieSearchResult("pqrpqs", 5, 3, 0, false, false)
        )

        Assertions.assertThat(resultF).containsExactlyInAnyOrder(
            TrieSearchResult("tu vw, xyz", 6, 2, 0, false, true)
        )

        Assertions.assertThat(resultG).isEmpty()
    }

    @Test
    fun testMatchBySubstringFuzzyWithSort() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("man", Unit)
        trie.put("manu", Unit)
        trie.put("many", Unit)
        trie.put("manual", Unit)
        trie.put("manually", Unit)
        trie.put("manuals", Unit)
        trie.put("linux manual", Unit)

        // Act
        val result = trie.matchBySubstringFuzzy("manual", 3)

        // Assert
        Assertions.assertThat(result).containsExactly(
            // matches whole sequence is highest ranking
            TrieSearchResult("manual", Unit, 6, 0, true, true),
            // matches a whole word
            TrieSearchResult("linux manual", Unit, 6, 0, false, true),
            // matches the highest possible number of characters, but it's neither the whole sequence nor a whole word
            TrieSearchResult("manuals", Unit, 6, 0, false, false),
            // same as above, but the string is longer, so is ranked lower
            TrieSearchResult("manually", Unit, 6, 0, false, false),
            // partial match, with fewer errors
            TrieSearchResult("manu", Unit, 4, 2, false, false),
            // partial match, with more errors
            TrieSearchResult("man", Unit, 3, 3, false, false),
            // partial match, with more errors, and lower string
            TrieSearchResult("many", Unit, 3, 3, false, false),
        )
    }


    @Test
    fun testMatchBySubstringFuzzy() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("goggle", Unit)
        trie.put("google", Unit)
        trie.put("googly", Unit)
        trie.put("giegly", Unit) // will not match any
        trie.put("gogle", Unit)  // missing letter
        trie.put("blah google blah", Unit) // good match with chars before and after

        // Act
        val resultOne = trie.matchBySubstringFuzzy("goggle", 1)
        val resultTwo = trie.matchBySubstringFuzzy("goggle", 2)

        // Assert
        Assertions.assertThat(resultOne).containsExactly(
            TrieSearchResult("goggle", Unit, 6, 0, true, true),
            TrieSearchResult("gogle", Unit, 5, 1, false, false),
            TrieSearchResult("google", Unit, 5, 1, false, false),
            TrieSearchResult("blah google blah", Unit, 5, 1, false, false)
        )
        Assertions.assertThat(resultTwo).containsExactly(
            TrieSearchResult("goggle", Unit, 6, 0, true, true),
            TrieSearchResult("gogle", Unit, 5, 1, false, false),
            TrieSearchResult("google", Unit, 5, 1, false, false),
            TrieSearchResult("blah google blah", Unit, 5, 1, false, false),
            TrieSearchResult("googly", Unit, 4, 2, false, false),
        )
    }

    @Test
    fun testFuzzySubstringSearchWorksBothWays() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("raphael", Unit)
        trie.put("rafael", Unit)

        // Act
        val r1 = trie.matchBySubstringFuzzy("raphael", 2)
        val r2 = trie.matchBySubstringFuzzy("rafael", 2)

        // Assert
        Assertions.assertThat(r1).containsExactly(
            TrieSearchResult("raphael", Unit, 7, 0, true, true),
            TrieSearchResult("rafael", Unit, 5, 2, false, false))
        Assertions.assertThat(r2).containsExactly(
            TrieSearchResult("rafael", Unit, 6, 0, true, true),
            TrieSearchResult("raphael", Unit, 5, 2, false, false))
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