## Trie

This project implements a `Trie`, a.k.a. `PrefixTree`, a data structure used for efficient string searching.

The `Trie` implemented here is thread-safe, unit-tested, and able to efficiently retrieve data using any of the following strategies:
  - exact match (like a `Map`)
  - prefix match
  - substring match
  - "fuzzy" substring match with configurable error tolerance:
    - searching for `brasil` with `errorTolerance=1` returns:
      - `brasil` (perfect match)
      - `brazil` (English - 1 wrong letter) 
      - `brésil` (French - 1 wrong letter)
      - `brasilien` (German - no wrong letters - additional letters at the end don't count as errors)
    - searching for `raphael` with `errorTolerance=2` returns:
      - `raphael` (perfect match)
      - `rafael` (1 wrong letter + 1 missing letter = 2 errors)
      - `raffaello` (2 wrong letters - additional letters at the end don't count as errors)
      - `raphaela` (match with no wrong letters - additional letters at the end don't count as errors)

More work needs to be done to filter out irrelevant results; for example, searching for `indices` with an `errorTolerance=2` will return `indistinguishable`! It's perfectly correct, but a bit unexpected. I'm thinking of counting additional letters, like in the examples above, as errors.

A demo of an `HtmlCrawler` has also been provided to illustrate the usage of the `Trie`.