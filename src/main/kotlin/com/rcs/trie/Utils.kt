package com.rcs.trie

import java.text.Normalizer

class Utils {

    companion object {

        private val wordSeparatorRegex = Regex("[\\s\\p{P}]")
        private val diacriticalMarksRegex = Regex("\\p{InCOMBINING_DIACRITICAL_MARKS}+")

        fun <T> TrieNode<T>.compare(
            that: String,
            matchingOptions: MatchingOptions
        ): TrieNodeMatchResult {

            val (caseInsensitive, diacriticInsensitive) = matchingOptions

            val caseInsensitiveMatch = if (caseInsensitive) {
                that.lowercase() ==
                        (this.lowercaseString ?: this.string)
            } else null

            val diacriticInsensitiveMatch = if (diacriticInsensitive) {
                that.removeDiacritics() ==
                        (this.withoutDiacriticsString ?: this.string)
            } else null

            val caseAndDiacriticInsensitiveMatch = if (caseInsensitive && diacriticInsensitive) {
                that.lowercase().removeDiacritics() ==
                        (this.lowercaseAndWithoutDiacriticsString ?: this.string)
            } else null

            return TrieNodeMatchResult(
                exactMatch = this.string == that,
                caseInsensitiveMatch = caseInsensitiveMatch,
                diacriticInsensitiveMatch = diacriticInsensitiveMatch,
                caseAndDiacriticInsensitiveMatch = caseAndDiacriticInsensitiveMatch
            )
        }

        fun String.isWordSeparator(): Boolean {
            return this == "" /* == root */ || this.matches(wordSeparatorRegex)
        }

        fun CharSequence.indexOfLastWordSeparator(endIndex: Int = this.length - 1): Int? {
            return (0..endIndex).reversed().firstOrNull {
                this[it].toString().matches(wordSeparatorRegex)
            }
        }

        fun CharSequence.indexOfFirstWordSeparator(startIndex: Int = 0): Int? {
            return (startIndex until this.length).firstOrNull {
                this[it].toString().matches(wordSeparatorRegex)
            }
        }

        fun String.isWordSeparatorAt(index: Int): Boolean {
            return index < 0 || index >= this.length || this[index].toString().isWordSeparator()
        }

        fun String.removeDiacritics(): String {
            return Normalizer.normalize(this, Normalizer.Form.NFD)
                .replace(diacriticalMarksRegex, "")
        }
    }
}