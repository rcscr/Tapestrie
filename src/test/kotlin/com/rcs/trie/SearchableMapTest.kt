package com.rcs.trie

import org.assertj.core.api.Assertions
import kotlin.test.Test

class SearchableMapTest {

    data class Dummy(val a: String, val b: String, val c: String) {
        val search = listOf(a, c) // purposefully ignores 'b'
    }

    @Test
    fun testMatchBySubstring() {
        // Arrange
        val target: SearchableMap<Int, Dummy> = SearchableMap { it.search }

        val dummy0 = Dummy("abc", "def", "ghi")
        val dummy1 = Dummy("abc", "jkl", "mno")
        val dummy2 = Dummy("abz", "pqr", "abc")

        target.put(1, dummy0) // should get overriden and unindexed below
        target.put(1, dummy1)
        target.put(2, dummy2)

        // Act
        val resultA: List<Dummy> = target.searchBySubstring("abc", 2)
        val resultB: List<Dummy> = target.searchBySubstring("mn0", 2)
        val resultC: List<Dummy> = target.searchBySubstring("pqr", 2)

        // Assert
        Assertions.assertThat(resultA).containsExactly(dummy1, dummy2)
        Assertions.assertThat(resultB).containsExactly(dummy1)
        Assertions.assertThat(resultC).isEmpty()
    }
}