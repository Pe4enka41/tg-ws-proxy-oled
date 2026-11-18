package com.amurcanov.tgwsproxy.ui

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── OLED palette ─────────────────────────────────────────────────────────────
// True black background, near-black raised surfaces, one muted signal color.
// No gradients, no glows — contrast comes from spacing and type, not effects.
object Ink {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0B0B0C)
    val SurfaceRaised = Color(0xFF141416)
    val Line = Color(0xFF1F1F22)
    val LineSoft = Color(0xFF151517)
    val Text = Color(0xFFF2F2F3)
    val TextDim = Color(0xFF8B8B92)
    val TextFaint = Color(0xFF5A5A61)
    val Signal = Color(0xFF2AABEE)
    val Warning = Color(0xFFE8B45F)
    val Danger = Color(0xFFFF6B6B)
}

private val OledScheme = darkColorScheme(
    primary = Ink.Text,
    onPrimary = Ink.Black,
    primaryContainer = Ink.SurfaceRaised,
    onPrimaryContainer = Ink.Text,
    secondary = Ink.Signal,
    onSecondary = Ink.Black,
    secondaryContainer = Color(0xFF0E2836),
    onSecondaryContainer = Ink.Signal,
    tertiary = Ink.TextDim,
    onTertiary = Ink.Black,
    background = Ink.Black,
    onBackground = Ink.Text,
    surface = Ink.Surface,
    onSurface = Ink.Text,
    surfaceVariant = Ink.SurfaceRaised,
    onSurfaceVariant = Ink.TextDim,
    outline = Ink.Line,
    outlineVariant = Ink.LineSoft,
    error = Ink.Danger,
    onError = Ink.Black,
    errorContainer = Color(0xFF2A1010),
    onErrorContainer = Ink.Danger,
    inverseSurface = Ink.Text,
    inverseOnSurface = Ink.Black,
    inversePrimary = Ink.Black,
    surfaceTint = Color.Transparent,
)

val TgWsProxyTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp
    ),
)

@Composable
fun TgWsProxyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Ink.Black.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                window.isNavigationBarContrastEnforced = false
                @Suppress("DEPRECATION")
                window.isStatusBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = OledScheme,
        typography = TgWsProxyTypography,
        content = content
    )
}
