package com.rcs.trie

import com.rcs.trie.FuzzySearchState.Companion.compare
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.*

class TrieFuzzySearchCompareTest {

    @Test
    fun testSimpleMatch() {
        // Arrange
        val trieNode = TrieNode("a", Unit, 0, mutableSetOf(), null)
        val searchChar = "a"
        val options = MatchingOptions.allDisabled

        // Act
        val result = trieNode.compare(searchChar, options)

        // Assert
        assertThat(result.exactMatch).isTrue()
        assertThat(result.anyMatch).isTrue()
        assertThat(result.caseInsensitiveMatch).isNull()
        assertThat(result.diacriticInsensitiveMatch).isNull()
        assertThat(result.caseAndDiacriticInsensitiveMatch).isNull()
    }

    @Test
    fun testSimpleNoMatch() {
        // Arrange
        val trieNode = TrieNode("a", Unit, 0, mutableSetOf(), null)
        val searchChar = "b"
        val options = MatchingOptions.allDisabled

        // Act
        val result = trieNode.compare(searchChar, options)

        // Assert
        assertThat(result.exactMatch).isFalse()
        assertThat(result.anyMatch).isFalse()
        assertThat(result.caseInsensitiveMatch).isNull()
        assertThat(result.diacriticInsensitiveMatch).isNull()
        assertThat(result.caseAndDiacriticInsensitiveMatch).isNull()
    }

    @Test
    fun testCaseInsensitiveMatch() {
        // Arrange
        val trieNode = TrieNode("a", Unit, 0, mutableSetOf(), null)
        val searchChar = "A"
        val options = MatchingOptions(
            caseInsensitive = true,
            diacriticInsensitive = false,
            wildcard = false,
        )

        // Act
        val result = trieNode.compare(searchChar, options)

        // Assert
        assertThat(result.exactMatch).isFalse()
        assertThat(result.anyMatch).isTrue()
        assertThat(result.caseInsensitiveMatch).isTrue()
        assertThat(result.diacriticInsensitiveMatch).isNull()
        assertThat(result.caseAndDiacriticInsensitiveMatch).isNull()
    }

    @Test
    fun testDiacriticInsensitiveMatch() {
        // Arrange
        val trieNode = TrieNode("a", Unit, 0, mutableSetOf(), null)
        val searchChar = "ã"
        val options = MatchingOptions(
            caseInsensitive = false,
            diacriticInsensitive = true,
            wildcard = false,
        )

        // Act
        val result = trieNode.compare(searchChar, options)

        // Assert
        assertThat(result.exactMatch).isFalse()
        assertThat(result.anyMatch).isTrue()
        assertThat(result.caseInsensitiveMatch).isNull()
        assertThat(result.diacriticInsensitiveMatch).isTrue()
        assertThat(result.caseAndDiacriticInsensitiveMatch).isNull()
    }

    @Test
    fun testCaseAndDiacriticInsensitiveMatch() {
        // Arrange
        val trieNode = TrieNode("a", Unit, 0, mutableSetOf(), null)
        val searchChar = "Á"
        val options = MatchingOptions(
            caseInsensitive = true,
            diacriticInsensitive = true,
            wildcard = false,
        )

        // Act
        val result = trieNode.compare(searchChar, options)

        // Assert
        assertThat(result.exactMatch).isFalse()
        assertThat(result.anyMatch).isTrue()
        assertThat(result.caseInsensitiveMatch).isFalse()
        assertThat(result.diacriticInsensitiveMatch).isFalse()
        assertThat(result.caseAndDiacriticInsensitiveMatch).isTrue()
    }
}