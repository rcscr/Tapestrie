# Tapes*trie*

This project implements a `Trie`, a.k.a. `PrefixTree`, a data structure used for efficient string searching.

The analogy of a `Trie` data structure to a tapestry is quite fitting. Just as a tapestry is composed of many threads woven together to create a complete piece of art, a `Trie` organizes various threads of strings to form a cohesive structure for efficient retrieval. Each path through the `Trie` can be thought of as a thread contributing to the overall functionality.

The `Trie` implemented here is thread-safe, unit-tested, and able to efficiently retrieve data using any of the following strategies:
  - exact match (like a `Map`)
  - prefix match
  - substring match
  - configurable case- and diacritic-insensitivity
  - fuzzy substring match with configurable error tolerance: Brasil will match Brazil; Raphael will match Rafael; etc

For fuzzy search, there are several different strategies:

- LIBERAL: matches everywhere in the string, and allows errors in the beginning, middle, and end
- EXACT_PREFIX: matches only words that start with the first letter of the keyword, regardless of error tolerance
- FUZZY_PREFIX: similar to EXACT_PREFIX, but allows the error tolerance at the beginning (not in the middle and end)
- FUZZY_POSTFIX: similar to EXACT_PREFIX, but allows the error tolerance only at the end (not in the beginning or middle)
- ADJACENT_SWAP: accepts only errors due to adjacent letter swaps (i.e. typos)
- SYMMETRICAL_SWAP: accepts only errors due to letter swaps anywhere in the string
- ACRONYM: matches strings containing words that form the acronym provided

### Demo

A demo of an `HtmlCrawler` & `HtmlSearcher` have also been provided to illustrate the usage of the `Trie`.

Searching the Linux manual (1,860 HTML pages and 21,181 unique tokens) for `computer` with `errorTolerance=2` takes ~1 second and will return HTML pages containing any of these hits:

<pre>
[computer, computers, computerr1, compute, computed, computes, compuserve, comput, compiler, compugen, competes, compilers, computing, computation, compatgroup, computations, recomputes, minicomputer, deepcomputing]
</pre>

Some results, like `competes`, might seem irrelevant, but they are still acceptable matches given the `errorTolerance=2`. Notice that, if you swap the first `e` and `s` for `u` and `r` respectively, you'll have your keyword - with only two errors, as required.

As you might have noticed, these results are sorted by best match, considering the following information:
    
- matchedSubstring (String): *the minimum portion of the string that matched the keyword*
- matchedWord (String): *the whole word where the match was found*
- numberOfMatches (Int): *number of characters that matched*
- numberOfErrors (Int): *number of errors due to misspelling or letters missing*
- numberOfCaseMismatches (Int) *if case-insensitive search, number of case mismatches*
- numberOfDiacriticMismatches (Int) *if diacritic-insensitive search, number of diacritic mismatches*
- prefixDistance (Int): *the distance from the start of the match to the beginning of the word*
- matchedWholeString (Boolean): *whether the keyword perfectly matched the entire string stored in the Trie*
- matchedWholeWord (Boolean): *whether the keyword perfectly matched a whole word within the string*

As an example, let's examine the best and worst search hits above:

#### Best
<pre>
{
    "string": "computer",
    "value": [
        {
            "url": "https://docs.huihoo.com/linux/man/20100621/htmlman8/agetty.8.html",
            "occurrences": 1
        },
        {
            "url": "https://docs.huihoo.com/linux/man/20100621/htmlman7/uri.7.html",
            "occurrences": 1
        },
        ... 17 more hits omitted
    ],
    "stats": {
        "matchedSubstring": "computer",
        "matchedWord": "computer",
        "numberOfMatches": 8,
        "numberOfErrors": 0,
        "numberOfCaseMismatches": 0,
        "numberOfDiacriticMismatches": 0,
        "prefixDistance": 0,
        "matchedWholeString": true,
        "matchedWholeWord": true
    }
}
</pre>

#### Worst
<pre>
{
    "string": "deepcomputing",
    "value": [
        {
            "url": "https://docs.huihoo.com/linux/man/20100621/htmlman2/spu_run.2.html",
            "occurrences": 1
        },
        {
            "url": "https://docs.huihoo.com/linux/man/20100621/htmlman2/spu_create.2.html",
            "occurrences": 1
        }
    ],
    "stats": {
        "matchedSubstring": "comput",
        "matchedWord": "deepcomputing",
        "numberOfMatches": 6,
        "numberOfErrors": 2,
        "numberOfCaseMismatches": 0,
        "numberOfDiacriticMismatches": 0,
        "prefixDistance": 4,
        "matchedWholeString": false,
        "matchedWholeWord": false
    }
}
</pre>

### Other notes

As an optimization, each node in the `Trie` stores its depth: the max size of a word stemming from it. This allowed me to implement a culling strategy to swiftly discard nodes whose strings are not long enough to provide a match. In general, searches for longer strings are faster, because fewer strings will be examined.

The example above is quite extreme with more than 20,000 strings. But even so, the fuzzy search took ~1 second, which is quite impressive. However, this in-memory `Trie` certainly has its limitations; for one, it is quite memory-intensive. In many scenarios, a solution like `ElasticSearch` should be used instead.

In general, the greater the error tolerance, the slower the performance, because there are more paths to explore. Furthermore, a shallow `Trie`, where each entry is short (i.e. words) offers the best performance, but with the limitation that you can only search for short strings. A `Trie` that stores longer text (i.e. sentences) allows searching for phrases (multiple words chained together), but is slower.

### Build & run demo

<pre>docker build . -t trie-demo</pre>

<pre>
docker run -p 4567:4567 --rm trie-demo
</pre>

Or just run `com.rcs.htmlcrawlerdemo.HtmlCrawlerDemo.kt`

<pre>
POST http://localhost:4567/search
Body: {"keyword": "computer", "strategy": "FUZZY", "errorTolerance": 2 }
</pre>
