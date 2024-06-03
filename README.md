## Trie

This project implements a `Trie`, a.k.a. `PrefixTree`, a data structure used for efficient string searching.

The `Trie` implemented here is thread-safe, unit-tested, and able to efficiently retrieve data using any of the following strategies:
  - exact match (like a `Map`)
  - prefix match
  - substring match
  - "fuzzy" substring match with configurable error tolerance: Brasil will match Brazil; Raphael will match Rafael; etc

### Demo

A demo of an `HtmlCrawler` has also been provided to illustrate the usage of the `Trie`.

Searching the Linux manual (1,860 HTML pages and 21,181 unique tokens) for `computer` with `errorTolerance=2` takes 25 seconds (on an i5 processor) and will return all of these hits:

<pre>
[computer, computers, computerr1, compute, computed, computes, compuserve, comput, compiler, compugen, competes, compilers, computing, computation, compatgroup, computations, recomputes, minicomputer, deepcomputing]
</pre>

Some results, like `competes`, might seem irrelevant, but they are still acceptable matches given the `errorTolerance=2`: if you swap out the first `e` and `s` for `u` and `r` respectively, you'll have your keyword - with only two errors, as required.

As you might have noticed, these results are sorted by best match, considering the following information:
    
- matchedSubstring (String): *the minimum portion of the string that matched the keyword*
- matchedWord (String): *the whole word where the match was found*
- numberOfMatches (Int): *number of characters that matched*
- numberOfErrors (Int): *number of errors due to misspelling or letters missing*
- prefixDistance (Int): *the distance from the start of the match to the beginning of the word*
- matchedWholeString (Boolean): *whether the keyword perfectly matched the entire string stored in the Trie*
- matchedWholeWord (Boolean): *whether the keyword perfectly matched a whole word within the string*

As an example, let's examine the best and worst search hits above:

#### Best
<pre>
TrieSearchResult(
    string=computer, 
    value=[HtmlIndexEntry(url=htmlman8/agetty.8.html, occurrences=2), HtmlIndexEntry(url=htmlman3/rtime.3.html, occurrences=2), HtmlIndexEntry(url=gfdl-3.html, occurrences=1), ...], 
    matchedSubstring=computer, 
    matchedWord=computer, 
    numberOfMatches=8, 
    numberOfErrors=0, 
    prefixDistance=0, 
    matchedWholeString=true, 
    matchedWholeWord=true
)
</pre>

#### Worst
<pre>
TrieSearchResult(
    string=deepcomputing, 
    value=[HtmlIndexEntry(url=htmlman2/spu_run.2.html, occurrences=1), HtmlIndexEntry(url=htmlman2/spu_create.2.html, occurrences=1)], 
    matchedSubstring=comput, 
    matchedWord=deepcomputing, 
    numberOfMatches=6, 
    numberOfErrors=2, 
    prefixDistance=4, 
    matchedWholeString=false, 
    matchedWholeWord=false
)
</pre>

### Other notes

The greater the error tolerance, the slower the performance. The same search with `errorTolerance=1` returned instantly, because there were fewer paths to explore.

This in-memory `Trie` certainly has its limitations. It's great for short amounts of data and for precise searches. The example above is quite extreme with more than 20,000 strings; and 25 seconds for a search, fuzzy or otherwise, is not exactly user-friendly. In real scenarios, a solution like `ElasticSearch` would be used instead.

A shallow `Trie`, where each entry is short (i.e. words) offers the best performance, but with the limitation that you can only search for short strings. A `Trie` that stores longer text (i.e. sentences) allows searching for phrases (multiple words chained together), but is slower.
