package com.rcs.trie

class SearchableMap<K, V>(private val searchTermsExtractor: (V) -> Collection<String>) {

    private val map: MutableMap<K, V> = mutableMapOf()

    // maps search terms to a set of keys in the map above
    private val trie: Trie<MutableSet<K>> = Trie()

    fun put(key: K, value: V) {
        val previousValue = map.put(key, value)
        removeIndex(previousValue)
        addIndex(value, key)
    }

    fun get(key: K): V? {
        return map[key]
    }

    fun searchBySubstring(search: String, minLength: Int): List<V> {
        return trie.matchBySubstring(search, minLength)
            .map { it.value }
            .flatten()
            .distinct()
            .map { map[it]!! }
    }

    private fun removeIndex(value: V?) {
        value?.let {
            searchTermsExtractor(it).forEach(trie::remove)
        }
    }

    private fun addIndex(value: V, key: K) {
        searchTermsExtractor(value)
            .forEach {
                val newKeys: MutableSet<K> = trie.getExactly(it) ?: mutableSetOf()
                newKeys.add(key)
                trie.put(it, newKeys)
            }
    }
}