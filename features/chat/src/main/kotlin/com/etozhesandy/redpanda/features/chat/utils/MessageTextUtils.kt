package com.etozhesandy.redpanda.features.chat.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

private val urlRegex = Regex("""https?://[^\s<>"']+""")

/** What a sentence leaves stuck to a link's end: «see https://vk.com/wall1_2.» is a link without the dot. */
private const val TRAILING_PUNCTUATION = ".,;:!?)]}»"

/**
 * The text of a message with each `http(s)` address in it made tappable.
 *
 * The addresses are found by the same pattern the export's own links follow, and what a sentence
 * puts after one (a full stop, a closing bracket) is left out of the link. A tap is handed to
 * [onLinkClick] with the address; deciding where it opens is not this function's business.
 */
fun String.withLinks(linkColor: Color, onLinkClick: (String) -> Unit): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in urlRegex.findAll(this@withLinks)) {
        val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
        if (url.length <= "https://".length) continue
        append(this@withLinks.substring(cursor, match.range.first))
        withLink(
            LinkAnnotation.Clickable(
                tag = url,
                styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                linkInteractionListener = { onLinkClick(url) },
            ),
        ) { append(url) }
        cursor = match.range.first + url.length
    }
    append(this@withLinks.substring(cursor))
}
