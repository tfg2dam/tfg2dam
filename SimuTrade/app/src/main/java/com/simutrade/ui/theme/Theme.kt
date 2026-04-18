package com.simutrade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🎨 COLORES BASE
private val Blue600 = Color(0xFF2563EB)
private val Blue100 = Color(0xFFDBEAFE)

private val Purple600 = Color(0xFF9333EA)

private val Green600 = Color(0xFF16A34A)
private val Green100 = Color(0xFFD1FAE5)

private val Red600 = Color(0xFFDC2626)

// ================= EXTENSIÓN PRO =================

// 👇 añadimos positive al sistema de colores
val ColorScheme.positive: Color
    get() = Green600

// ================= LIGHT =================

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    primaryContainer = Blue100,

    secondary = Purple600,

    error = Red600,

    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onError = Color.White,

    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

// ================= DARK =================

private val DarkColorScheme = darkColorScheme(
    primary = Blue600,
    primaryContainer = Color(0xFF1E3A8A),

    secondary = Purple600,

    error = Red600,

    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onError = Color.White,

    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1),
)

// ================= THEME =================

@Composable
fun SimuTradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}