package com.shalltear.shallnotspend.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.shalltear.shallnotspend.model.ThemePalette

private data class PaletteTokens(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

private fun paletteTokensFor(themePalette: ThemePalette): PaletteTokens {
    return when (themePalette) {
        ThemePalette.NEON_MINT -> PaletteTokens(
            primary = Color(0xFF00FF7F),
            secondary = Color(0xFFFF5252),
            tertiary = Color(0xFF9BFFB8)
        )

        ThemePalette.SUNSET_CORAL -> PaletteTokens(
            primary = Color(0xFFFF8A5B),
            secondary = Color(0xFFFF4F7B),
            tertiary = Color(0xFFFFC371)
        )

        ThemePalette.OCEAN_BLUE -> PaletteTokens(
            primary = Color(0xFF43C6FF),
            secondary = Color(0xFF5B8CFF),
            tertiary = Color(0xFF8AE3FF)
        )
    }
}

@Composable
fun ShantSpendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ThemePalette = ThemePalette.NEON_MINT,
    // Disable dynamic colors to enforce the cool custom theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val paletteTokens = paletteTokensFor(palette)
    val darkColorScheme = darkColorScheme(
        primary = paletteTokens.primary,
        secondary = paletteTokens.secondary,
        tertiary = paletteTokens.tertiary,
        background = BackgroundDark,
        surface = SurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary
    )
    val lightColorScheme = lightColorScheme(
        primary = paletteTokens.primary,
        secondary = paletteTokens.secondary,
        tertiary = paletteTokens.tertiary,
        background = BackgroundLight,
        surface = SurfaceLight,
        surfaceVariant = Color(0xFFE0E0E0),
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = TextPrimaryLight,
        onSurface = TextPrimaryLight,
        onSurfaceVariant = Color(0xFF555555)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}