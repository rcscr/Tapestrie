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
    fun testRemoveNonExistant() {
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
            TrieSearchResult("abcdef", 1, 1, 0, 0, false, false)
        )

        assertThat(resultB).containsExactlyInAnyOrder(
            TrieSearchResult("abcdef", 1, 3, 0, 3, false, false),
            TrieSearchResult("defghi", 2, 3, 0, 0, false, false)
        )

        assertThat(resultC).containsExactlyInAnyOrder(
            TrieSearchResult("defghi", 2, 3, 0, 3, false, false),
            TrieSearchResult("deghij", 3, 3, 0, 2, false, false)
        )

        assertThat(resultD).containsExactlyInAnyOrder(
            TrieSearchResult("jklmno", 4, 6, 0, 0, true, true)
        )

        assertThat(resultE).containsExactlyInAnyOrder(
            TrieSearchResult("pqrpqs", 5, 3, 0, 3, false, false)
        )

        assertThat(resultF).containsExactlyInAnyOrder(
            TrieSearchResult("tu vw, xyz", 6, 2, 0, 0, false, true)
        )

        assertThat(resultG).isEmpty()
    }

    @Test
    fun testDoesNotMatchEdgeCase() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("ionice", Unit)

        // Act
        val result = trie.matchBySubstringFuzzy("indices", 2, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun testMatchBySubstringFuzzyAnchorToPrefix() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("index", Unit)
        trie.put("ondex", Unit) // will match because it has 1 wrong first letter
        trie.put("oldex", Unit) // will match because it has 2 wrong first letter
        trie.put("omtex", Unit) // will match because it has 2 wrong first letter
        trie.put("lalala index", Unit)
        trie.put("lalala ondex", Unit) // will match because it has 1 wrong first letter
        trie.put("lalala oldex", Unit) // will match because it has 2 wrong first letter
        trie.put("lalala omtex", Unit) // will not match because it has 3 wrong first letter

        // Act
        val result = trie.matchBySubstringFuzzy("index", 2, FuzzySubstringMatchingStrategy.ANCHOR_TO_PREFIX)

        // Assert
        assertThat(result).containsExactly(
            TrieSearchResult("index", Unit, 5, 0, 0, true, true),
            TrieSearchResult("lalala index", Unit, 5, 0, 0, false, true),
            TrieSearchResult("ondex", Unit, 4, 1, 1, false, false),
            TrieSearchResult("lalala ondex", Unit, 4, 1, 1, false, false),
            TrieSearchResult("oldex", Unit, 3, 2, 2, false, false),
            TrieSearchResult("lalala oldex", Unit, 3, 2, 2, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzyMatchPrefix() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("lalala index", Unit)
        trie.put("lalala indix", Unit)
        trie.put("lalala ondex", Unit) // will not match because it doesn't match first letter in keyword

        // Act
        val result = trie.matchBySubstringFuzzy("index", 1, FuzzySubstringMatchingStrategy.MATCH_PREFIX)

        // Assert
        assertThat(result).containsExactly(
            TrieSearchResult("lalala index", Unit, 5, 0, 0, false, true),
            TrieSearchResult("lalala indix", Unit, 4, 1, 0, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzyCommonCase() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("indexes", Unit)
        trie.put("indices", Unit)

        // Act
        val resultIndexes = trie.matchBySubstringFuzzy("indexes", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val resultIndices = trie.matchBySubstringFuzzy("indices", 2, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(resultIndexes).containsExactly(
            TrieSearchResult("indexes", Unit, 7, 0, 0, true, true),
            TrieSearchResult("indices", Unit, 5, 2, 0, false, false)
        )
        assertThat(resultIndices).containsExactly(
            TrieSearchResult("indices", Unit, 7, 0, 0, true, true),
            TrieSearchResult("indexes", Unit, 5, 2, 0, false, false)
        )
    }

    @Test
    fun testMatchBySubstringFuzzyWithSort() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("man", Unit)
        trie.put("manu", Unit)
        trie.put("many", Unit)
        trie.put("manual", Unit)
        trie.put("emanuel", Unit)
        trie.put("lemanuel", Unit)
        trie.put("manually", Unit)
        trie.put("manuals", Unit)
        trie.put("linux manual", Unit)

        // Act
        val result = trie.matchBySubstringFuzzy("manual", 3, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(result).containsExactly(
            // matches whole sequence is highest ranking
            TrieSearchResult("manual", Unit, 6, 0, 0, true, true),
            // matches a whole word
            TrieSearchResult("linux manual", Unit, 6, 0, 0, false, true),
            // matches the highest possible number of characters, but it's neither the whole sequence nor a whole word
            TrieSearchResult("manuals", Unit, 6, 0, 0, false, false),
            // same as above, but the string is longer, so is ranked lower
            TrieSearchResult("manually", Unit, 6, 0, 0, false, false),
            // partial match, with fewer errors
            TrieSearchResult("manu", Unit, 4, 2, 0, false, false),
            // partial match, with more errors
            TrieSearchResult("man", Unit, 3, 3, 0, false, false),
            // partial match, with more errors, and lower string
            TrieSearchResult("many", Unit, 3, 3, 0, false, false),
            // prefix match > 0
            TrieSearchResult("emanuel", Unit, 5, 1, 1, false, false),
            // prefix match > 1
            TrieSearchResult("lemanuel", Unit, 5, 1, 2, false, false)
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
        val resultOne = trie.matchBySubstringFuzzy("goggle", 1, FuzzySubstringMatchingStrategy.LIBERAL)
        val resultTwo = trie.matchBySubstringFuzzy("goggle", 2, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(resultOne).containsExactly(
            TrieSearchResult("goggle", Unit, 6, 0, 0, true, true),
            TrieSearchResult("gogle", Unit, 5, 1, 0, false, false),
            TrieSearchResult("google", Unit, 5, 1, 0, false, false),
            TrieSearchResult("blah google blah", Unit, 5, 1, 0, false, false)
        )
        assertThat(resultTwo).containsExactly(
            TrieSearchResult("goggle", Unit, 6, 0, 0, true, true),
            TrieSearchResult("gogle", Unit, 5, 1, 0, false, false),
            TrieSearchResult("google", Unit, 5, 1, 0, false, false),
            TrieSearchResult("blah google blah", Unit, 5, 1, 0, false, false),
            TrieSearchResult("googly", Unit, 4, 2, 0, false, false),
        )
    }

    @Test
    fun testFuzzySubstringSearchLiberal() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("this is raphael", Unit)
        trie.put("this is raphaël", Unit)
        trie.put("this is rafael", Unit)
        trie.put("this is rafaela", Unit)
        trie.put("this is raffaello", Unit)
        trie.put("this is raffaella", Unit)

        // Act
        val r1 = trie.matchBySubstringFuzzy("raphael", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val r2 = trie.matchBySubstringFuzzy("rafael", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val r3 = trie.matchBySubstringFuzzy("raffaello", 2, FuzzySubstringMatchingStrategy.LIBERAL)
        val r4 = trie.matchBySubstringFuzzy("raffaello", 3, FuzzySubstringMatchingStrategy.LIBERAL)
        val r5 = trie.matchBySubstringFuzzy("raffaello", 4, FuzzySubstringMatchingStrategy.LIBERAL)

        // Assert
        assertThat(r1).containsExactly(
            TrieSearchResult("this is raphael", Unit, 7, 0, 0, false, true),
            TrieSearchResult("this is raphaël", Unit, 6, 1, 0, false, false),
            TrieSearchResult("this is rafael", Unit, 5, 2, 0, false, false),
            TrieSearchResult("this is rafaela", Unit, 5, 2, 0, false, false),
            TrieSearchResult("this is raffaello", Unit, 5, 2, 0, false, false),
            TrieSearchResult("this is raffaella", Unit, 5, 2, 0, false, false))
        assertThat(r2).containsExactly(
            TrieSearchResult("this is rafael", Unit, 6, 0, 0, false, true),
            TrieSearchResult("this is rafaela", Unit, 6, 0, 0, false, false),
            TrieSearchResult("this is raffaello", Unit, 6, 1, 0, false, false),
            TrieSearchResult("this is raffaella", Unit, 6, 1, 0, false, false),
            TrieSearchResult("this is raphael", Unit, 5, 2, 0, false, false))
        assertThat(r3).containsExactly(
            TrieSearchResult("this is raffaello", Unit, 9, 0, 0, false, true),
            TrieSearchResult("this is raffaella", Unit, 8, 1, 0, false, false))
        assertThat(r4).containsExactly(
            TrieSearchResult("this is raffaello", Unit, 9, 0, 0, false, true),
            TrieSearchResult("this is raffaella", Unit, 8, 1, 0, false, false),
            TrieSearchResult("this is rafael", Unit, 6, 3, 0, false, false),
            TrieSearchResult("this is rafaela", Unit, 6, 3, 0, false, false))
        assertThat(r5).containsExactly(
            TrieSearchResult("this is raffaello", Unit, 9, 0, 0, false, true),
            TrieSearchResult("this is raffaella", Unit, 8, 1, 0, false, false),
            TrieSearchResult("this is rafael", Unit, 6, 3, 0, false, false),
            TrieSearchResult("this is rafaela", Unit, 6, 3, 0, false, false),
            TrieSearchResult("this is raphael", Unit, 5, 4, 0, false, false))
    }

    @Test
    fun testMatchesLongWord() {
        // Arrange
        val trie = Trie<Unit>()
        trie.put("indistinguishable", Unit)

        // Act
        val result = trie.matchBySubstringFuzzy("indices", 2, FuzzySubstringMatchingStrategy.MATCH_PREFIX)

        // Assert
        assertThat(result).containsExactly(
            TrieSearchResult("indistinguishable", Unit, 5, 2, 0, false, false)
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