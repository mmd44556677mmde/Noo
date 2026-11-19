package com.etozhesandy.redpanda.features.chat.utils

private const val VOWELS = "аеиоуыэюяё"

// Each list is tried longest ending first, so «ившись» is never mistaken for «сь».
private fun endings(vararg words: String): List<String> = words.sortedByDescending { it.length }

// The endings of the Snowball stemmer for Russian (Porter's algorithm for the language).
private val PERFECTIVE_GERUND_AFTER_A = endings("в", "вши", "вшись")
private val PERFECTIVE_GERUND = endings("ив", "ивши", "ившись", "ыв", "ывши", "ывшись")
private val REFLEXIVE = endings("ся", "сь")
private val ADJECTIVE = endings(
    "ее", "ие", "ые", "ое", "ими", "ыми", "ей", "ий", "ый", "ой", "ем", "им", "ым", "ом",
    "его", "ого", "ему", "ому", "их", "ых", "ую", "юю", "ая", "яя", "ою", "ею",
)
private val PARTICIPLE_AFTER_A = endings("ем", "нн", "вш", "ющ", "щ")
private val PARTICIPLE = endings("ивш", "ывш", "ующ")
private val VERB_AFTER_A = endings(
    "ла", "на", "ете", "йте", "ли", "й", "л", "ем", "н", "ло", "но", "ет", "ют", "ны", "ть", "ешь", "нно",
)
private val VERB = endings(
    "ила", "ыла", "ена", "ейте", "уйте", "ите", "или", "ыли", "ей", "уй", "ил", "ыл", "им", "ым", "ен",
    "ило", "ыло", "ено", "ят", "ует", "уют", "ит", "ыт", "ены", "ить", "ыть", "ишь", "ую", "ю",
)
private val NOUN = endings(
    "а", "ев", "ов", "ие", "ье", "е", "иями", "ями", "ами", "еи", "ии", "и", "ией", "ей", "ой", "ий", "й",
    "иям", "ям", "ием", "ем", "ам", "ом", "о", "у", "ах", "иях", "ях", "ы", "ь", "ю", "ию", "ью", "я", "ия", "ья",
)
private val SUPERLATIVE = endings("ейше", "ейш")

/**
 * The stem of a Russian word, so that «пальцем», «пальца» and «пальцы» meet at «пальц».
 *
 * This is the Snowball algorithm for Russian: the endings are cut only inside the region after the
 * word's first vowel, in the fixed order of its steps. A word that is not wholly lowercase Cyrillic,
 * or is shorter than four letters, comes back as it was — there is little to cut off, and a stem of
 * two letters would match half of the dictionary.
 */
fun stemRussian(input: String): String {
    if (input.length < 4 || input.any { it !in 'а'..'я' && it != 'ё' }) return input

    var word = input
    val rvStart = word.indexOfFirst { it in VOWELS }.let { if (it < 0) word.length else it + 1 }
    val r1Start = regionStart(word, 0)
    val r2Start = regionStart(word, r1Start)

    /** The longest of [candidates] that ends the word inside RV, [afterA] needing an «а»/«я» before it. */
    fun ending(candidates: List<String>, afterA: Boolean = false): String? = candidates.firstOrNull { suffix ->
        val start = word.length - suffix.length
        word.endsWith(suffix) && if (afterA) start - 1 >= rvStart && word[start - 1] in "ая" else start >= rvStart
    }

    fun cut(suffix: String) {
        word = word.dropLast(suffix.length)
    }

    val gerund = ending(PERFECTIVE_GERUND_AFTER_A, afterA = true) ?: ending(PERFECTIVE_GERUND)
    if (gerund != null) {
        cut(gerund)
    } else {
        ending(REFLEXIVE)?.let(::cut)
        val adjective = ending(ADJECTIVE)
        if (adjective != null) {
            cut(adjective)
            (ending(PARTICIPLE_AFTER_A, afterA = true) ?: ending(PARTICIPLE))?.let(::cut)
        } else {
            val verb = ending(VERB_AFTER_A, afterA = true) ?: ending(VERB)
            if (verb != null) cut(verb) else ending(NOUN)?.let(::cut)
        }
    }

    if (word.endsWith("и") && word.length - 1 >= rvStart) cut("и")

    for (suffix in listOf("ость", "ост")) {
        if (word.endsWith(suffix) && word.length - suffix.length >= r2Start) {
            cut(suffix)
            break
        }
    }

    ending(SUPERLATIVE)?.let { cut(it) }
    if (word.endsWith("нн")) cut("н") else if (word.endsWith("ь") && word.length - 1 >= rvStart) cut("ь")

    return word
}

/** Where the region after the first «vowel followed by a consonant» pair of [word], from [from], begins. */
private fun regionStart(word: String, from: Int): Int {
    for (i in from until word.length - 1) {
        if (word[i] in VOWELS && word[i + 1] !in VOWELS) return i + 2
    }
    return word.length
}
