## Trie

This project implements a `Trie`, a.k.a. `PrefixTree`, a data structure used for efficient string searching.

The `Trie` implemented here is:

- thread-safe
- unit-tested
- able to efficiently retrieve data using any of the following strategies:
  - exact match (like a `Map`)
  - prefix match
  - substring match
  - "fuzzy" substring match with configurable error tolerance:
    - searching for `brazil` with `errorTolerance=1` returns:
      - `brazil` (perfect match)
      - `brasil` (1 wrong letter) 
    - searching for `raphael` with `errorTolerance=2` returns:
      - `raphael` (perfect match)
      - `rafael` (1 wrong letter, 1 missing letter = 2 errors)
    - it works the other way around, too