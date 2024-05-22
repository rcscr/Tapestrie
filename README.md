## Trie

This project implements a `Trie`, a.k.a. `PrefixTree`, a data structure used for efficient string searching.

The `Trie` implemented here is:

- thread-safe
- unit-tested
- able to search within a margin of error anywhere in the string:
  - searching for `goggle` with an error tolerance of 1 will return `google` (1 wrong letter) and `moggle` (1 wrong letter) but not `googly` (2 wrong letters)
  - searching for `goggle` with an error tolerance of 2 will return `google` (1 wrong letter), `moggle` (1 wrong letter), `googly` (2 wrong letters), but not `giegly` (3 wrong letters)