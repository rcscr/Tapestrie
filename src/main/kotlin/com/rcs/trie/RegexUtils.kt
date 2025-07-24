package com.rcs.trie

import java.text.Normalizer

class RegexUtils {

    companion object {

        private val wordSeparatorRegex = Regex("[\\s\\p{P}]")
        private val diacriticalMarksRegex = Regex("\\p{InCOMBINING_DIACRITICAL_MARKS}+")

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