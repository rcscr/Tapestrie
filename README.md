## Trie

This repo implements a Trie, a.k.a. prefix tree, a data structure used for efficient string searching.

The Trie implemented here is:

- thread-safe
- compacted
- unit-tested
- able to search within a margin of error (in the postfix only; i.e. searching for "googly" with a minimum of 5 matched chars will return "google")

An implementation of a SearchableMap is also provided as a proof of concept.

Future work includes:

- enable searching within a margin of error anywhere in the string (i.e. searching for "goggle" with a minimum of 5 matched chars should find "google")

This work was originally started in my other project, [RcsStockAppBack](https://github.com/raphael-correa-ng/RcsStockAppBack), as a way to efficiently search for stocks based on symbol and description. I felt that this data structure was interesting, so I created this new repo to maintain it.