## Trie

This project implements a `Trie`, a.k.a. `PrefixTree`, a data structure used for efficient string searching.

The `Trie` implemented here is thread-safe, unit-tested, and able to efficiently retrieve data using any of the following strategies:
  - exact match (like a `Map`)
  - prefix match
  - substring match
  - "fuzzy" substring match with configurable error tolerance: Brasil will match Brazil; Raphael will match Rafael; etc

A demo of an `HtmlCrawler` has also been provided to illustrate the usage of the `Trie`.

Searching the Linux manual (1,860 HTML pages and 21,181 unique tokens) for `indices` with `errorTolerance=2` takes only 1.5 minutes and will return all of these hits:

<pre>
[indices, indic, indexes, indicate, indirect, indicates, indicated, indicator, indicating, indication, indicators, indirectly, inacessible, inaccessble, inaccessible, indirections, indistinguishable, bindings, bindresvport]
</pre>

The first three hits are intuitive matches, but then the results become less relevant further down the list. However, these are still perfect matches given the `errorTolerance=2`.

As you might have noticed, these results are sorted by best match, considering the following information:
    
- matchSubstring (String): *the minimum portion of the string that matched the keyword, including errors*
- lengthOfMatch (Int): *number of characters that matched*
- errors (Int): *number of characters that didn't match due to misspelling or letters missing*
- prefixDistance (Int): *the distance from the start of the match to the beginning of the word*
- wordLength (Int): *the length of the word where the match was found*
- matchedWholeSequence (Boolean): *whether the keyword perfectly matched the entire string stored in the Trie*
- matchedWholeWord (Boolean): *whether the keyword perfectly matched a whole word*

As an example, let's examine the last search hit above:

<pre>
TrieSearchResult(
    string=bindresvport, 
    value=[index3.html, htmlman3/bindresvport.3.html], 
    matchSubstring=indres, 
    lengthOfMatch=5, 
    errors=2, 
    prefixDistance=1, 
    wordLength=12, 
    matchedWholeSequence=false, 
    matchedWholeWord=false
)
</pre>

The greater the error tolerance, the slowest the performance. The same search with `errorTolerance=1` returned instantly, because there were no matches other than `indices`.

A shallow `Trie`, where each entry is short (i.e. words) offers the best performance, but with the limitation that you can only search for short strings. A `Trie` that stores longer text (i.e. sentences) allows searching for phrases (multiple words chained together), but is slower.