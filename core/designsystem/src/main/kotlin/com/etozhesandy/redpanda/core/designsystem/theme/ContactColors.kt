package com.etozhesandy.redpanda.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ContactColorsLight = listOf(
    Color(0xFFC62828),
    Color(0xFFAD1457),
    Color(0xFF6A1B9A),
    Color(0xFF4527A0),
    Color(0xFF283593),
    Color(0xFF1565C0),
    Color(0xFF00838F),
    Color(0xFF00695C),
    Color(0xFF2E7D32),
    Color(0xFF9E7700),
    Color(0xFFD84315),
    Color(0xFF5D4037),
)

private val ContactColorsDark = listOf(
    Color(0xFFEF9A9A),
    Color(0xFFF48FB1),
    Color(0xFFCE93D8),
    Color(0xFFB39DDB),
    Color(0xFF9FA8DA),
    Color(0xFF90CAF9),
    Color(0xFF80DEEA),
    Color(0xFF80CBC4),
    Color(0xFFA5D6A7),
    Color(0xFFFFE082),
    Color(0xFFFFAB91),
    Color(0xFFBCAAA4),
)

/**
 * A color that stays the same for one [key] (e.g. a dialog id) so a contact can be recognised by
 * its color alone. The hash is computed by hand rather than via `String.hashCode()` so the
 * mapping can't shift with the platform.
 */
@Composable
fun contactColor(key: String): Color {
    val palette = if (isSystemInDarkTheme()) ContactColorsDark else ContactColorsLight
    var hash = 17
    for (char in key) hash = hash * 31 + char.code
    return palette[(hash and Int.MAX_VALUE) % palette.size]
}
