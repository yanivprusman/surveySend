package com.automatelinux.surveySend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The app's own palette, not Material's default.
 *
 * Two colours carry the whole identity:
 *
 *   INK   a deep indigo. Calm and professional — this is a tool used in front of
 *         a customer, and the default Material purple reads as an unfinished
 *         app, which is exactly the impression not to give while standing in
 *         someone's garden.
 *
 *   GOLD  #F5A623, the star colour — and deliberately the SAME hex the
 *         customer-facing survey page uses for its stars (survey's
 *         globals.css, --star). The two halves of this system are seen by
 *         different people minutes apart; a rating that is gold on his phone
 *         and orange on yours is two products.
 *
 * Gold is used for ratings ONLY. It never becomes a button or a heading: it
 * means "this is a score", and spending it on decoration would cost that.
 */
object SurveyColors {
    val Gold = Color(0xFFF5A623)
    val GoldDim = Color(0xFF6B5320)
    /** An unlit star. Visible enough to show how many are missing, quiet
     *  enough not to compete with the lit ones. */
    val StarOffLight = Color(0xFFD6D9DE)
    val StarOffDark = Color(0xFF3A4152)
}

private val Ink = Color(0xFF2C3E63)
private val InkLight = Color(0xFFA9BCE8)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE4F5),
    onPrimaryContainer = Color(0xFF16223D),
    secondary = Color(0xFF4A5A7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F0E6),
    onSecondaryContainer = Color(0xFF1B3D24),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1B1F27),
    surface = Color.White,
    onSurface = Color(0xFF1B1F27),
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = Color(0xFF5B6472),
    outline = Color(0xFFC7CCD6),
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E4),
    onErrorContainer = Color(0xFF7A1710),
)

private val DarkColors = darkColorScheme(
    primary = InkLight,
    onPrimary = Color(0xFF16223D),
    primaryContainer = Color(0xFF2C3E63),
    onPrimaryContainer = Color(0xFFDDE4F5),
    secondary = Color(0xFF9FB0CC),
    onSecondary = Color(0xFF16223D),
    secondaryContainer = Color(0xFF1F3A28),
    onSecondaryContainer = Color(0xFFC9EBD2),
    background = Color(0xFF11151F),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF181D29),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF242B3A),
    onSurfaceVariant = Color(0xFF9AA3B4),
    outline = Color(0xFF3A4152),
    error = Color(0xFFFF9E92),
    onError = Color(0xFF5C1209),
    errorContainer = Color(0xFF5C1209),
    onErrorContainer = Color(0xFFFFDAD5),
)

/** The unlit-star colour for the current scheme. */
@Composable
fun starOffColor(): Color =
    if (isSystemInDarkTheme()) SurveyColors.StarOffDark else SurveyColors.StarOffLight

// No Android-only dynamic colour, so this compiles for iOS too — and so the
// identity above survives contact with a phone whose wallpaper is bright pink.
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
