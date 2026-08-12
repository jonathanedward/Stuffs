package com.stampbook.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Passport palette: document navy, embossed gold, aged paper.
val PassportNavy = Color(0xFF1B3A6B)
val PassportNavyDeep = Color(0xFF12233F)
val PassportGold = Color(0xFFB08D3F)
val PassportOxblood = Color(0xFF7B2233)
val PaperLight = Color(0xFFF5F1E8)
val PaperCard = Color(0xFFFBF8F1)
val InkDark = Color(0xFF11151C)
val InkSurface = Color(0xFF1A1F29)

private val LightColors = lightColorScheme(
    primary = PassportNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4F2),
    onPrimaryContainer = PassportNavyDeep,
    secondary = PassportGold,
    onSecondary = Color.White,
    tertiary = PassportOxblood,
    onTertiary = Color.White,
    background = PaperLight,
    onBackground = Color(0xFF20242B),
    surface = PaperCard,
    onSurface = Color(0xFF20242B),
    surfaceVariant = Color(0xFFE7E1D4),
    onSurfaceVariant = Color(0xFF565B63),
    outline = Color(0xFFB6AF9E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C3EC),
    onPrimary = Color(0xFF0E2347),
    primaryContainer = Color(0xFF27405F),
    onPrimaryContainer = Color(0xFFD7E3F7),
    secondary = Color(0xFFD8BC77),
    onSecondary = Color(0xFF3A2E10),
    tertiary = Color(0xFFE3A0AC),
    onTertiary = Color(0xFF4B1220),
    background = InkDark,
    onBackground = Color(0xFFE3E2DD),
    surface = InkSurface,
    onSurface = Color(0xFFE3E2DD),
    surfaceVariant = Color(0xFF2A2F39),
    onSurfaceVariant = Color(0xFFB8BDC6),
    outline = Color(0xFF5C6270),
)

/** Serif headings read like a printed document; body stays the system sans. */
private val PassportTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif),
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 1.4.sp,
        ),
    )
}

@Composable
fun StampbookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = PassportTypography, content = content)
}
