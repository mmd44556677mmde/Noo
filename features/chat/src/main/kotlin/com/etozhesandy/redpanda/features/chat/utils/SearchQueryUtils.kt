package com.etozhesandy.redpanda.features.chat.utils

/**
 * Turns raw user input into an FTS4 prefix query over the stems of its words: `пальцем в небо`
 * becomes `пальц* неб*`, which matches the messages holding a word that starts with each stem —
 * «пальца», «пальцы» and «неба», «небо» alike.
 *
 * The prefix operator `*` only applies to a bare token in FTS3/FTS4 — quoting the term as
 * `"архив"*` makes it an exact-word match instead, so the term cannot be protected from FTS
 * syntax (`NEAR(`, `^`, `-`, `"` and the like fail the whole query with a syntax error) by
 * quoting it. Instead each run of letters and digits is taken as one token, mirroring how the
 * `unicode61` tokenizer of the `messages_fts` table splits the indexed text; everything else
 * is dropped, so no character of the input can reach the FTS parser as syntax. Input that holds no
 * token at all yields an empty query.
 *
 * Every term must be present — the terms are joined by the implicit AND of FTS.
 */
internal fun String.asPrefixQuery(): String = searchPrefixes().joinToString(" ") { "$it*" }

/**
 * The same terms as [asPrefixQuery], but a message needs only one of them: the fallback for a
 * question that no message answers as a whole.
 */
internal fun String.asRelaxedPrefixQuery(): String = searchPrefixes().joinToString(" OR ") { "$it*" }

/** How many distinct terms the query asks for; a relaxed search only differs from a strict one above one. */
internal fun String.searchTermCount(): Int = searchPrefixes().size

/** Anything that is not a letter or a digit is a token boundary, as it is for `unicode61`. */
private val TOKEN_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

private fun String.searchPrefixes(): List<String> {
    val tokens = split(TOKEN_SEPARATORS).filter { it.isNotEmpty() }.map { it.lowercase() }
    // A question made of nothing but filler words — «и», «в» — still searches for what it says.
    val meaningful = tokens.filterNot { it in STOP_WORDS }.ifEmpty { tokens }
    return meaningful.map(::prefixOf).distinct()
}

/**
 * The beginning of a word that its other forms share. The stem itself is shortened a little more:
 * the last letters of Russian words come and go with the form («потолок» — «потолка»), and a prefix
 * that stops before them finds both.
 */
private fun prefixOf(word: String): String {
    val stem = stemRussian(word)
    val dropped = when {
        stem.length >= 7 -> 2
        stem.length == 6 -> 1
        else -> 0
    }
    return if (stem.any { it in 'а'..'я' || it == 'ё' }) stem.dropLast(dropped) else stem
}

/** Words that carry no meaning of their own, left out of a search so they cannot demand a match. */
private val STOP_WORDS = setOf(
    "в", "во", "на", "с", "со", "к", "ко", "по", "о", "об", "от", "до", "из", "за", "у", "над", "под", "про",
    "без", "для", "при", "через", "между", "и", "а", "но", "да", "или", "не", "ни", "бы", "ли", "же", "что",
    "как", "это", "то", "так", "я", "мы", "ты", "вы", "он", "она", "оно", "они", "мне", "меня", "тебя",
    "его", "ее", "её", "их", "нас", "вас", "ему", "ей", "им", "вам",
)
