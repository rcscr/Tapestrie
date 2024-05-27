## Trie

This project implements a `Trie`, a.k.a. `PrefixTree`, a data structure used for efficient string searching.

The `Trie` implemented here is thread-safe, unit-tested, and able to efficiently retrieve data using any of the following strategies:
  - exact match (like a `Map`)
  - prefix match
  - substring match
  - "fuzzy" substring match with configurable error tolerance: Brasil will match Brazil; Raphael will match Rafael; etc

A demo of an `HtmlCrawler` has also been provided to illustrate the usage of the `Trie`.

Searching the Linux manual (1,860 HTML pages and 21,181 unique tokens) for `indices` with `errorTolerance=2` takes only 1.5 minutes and will return all of these hits, sorted by best match:

[indices, indic, indexes, indicate, indirect, indicates, indicated, indicator, indicating, indication, indicators, indirectly, inacessible, inaccessble, inaccessible, indirections, indistinguishable, bindings, bindresvport]

The first three hits are intuitive matches, but then the results become less relevant further down the list. However, these are still perfect matches given the `errorTolerance=2`.

The greater the error tolerance, the slowest the performance. The same search with `errorTolerance=1` returned instantly, because there were no matches other than `indices`.
