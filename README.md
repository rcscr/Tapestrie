## Trie

This repo implements the Trie, a.k.a. prefix tree, a data structure used for efficient string searching.

The Trie implemented here is:

- thread-safe
- compacted
- unit-tested
- able to search within a margin of error (in the postfix only; i.e. searching for "googly" with a minimum of 5 matched chars will return "google")

Future work includes:

- enable searching within a margin of error anywhere in the string (i.e. searching for "goggle" with a minimum of 5 matched chars should find "google")