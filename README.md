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
    - searching for `goggle` with `errorTolerance=1` returns:
      - `google` (1 wrong letter)
      - `moggle` (1 wrong letter) 
      - `gogle` (1 missing letter)
      - but not `googly` (2 wrong letters)
    - searching for `goggle` with `errorTolerance=2` returns:
      - `google` (1 wrong letter)
      - `moggle` (1 wrong letter)
      - `googly` (2 wrong letters)
      - `gole` (2 missing letters)
      - but not `giegly` (3 wrong letters)