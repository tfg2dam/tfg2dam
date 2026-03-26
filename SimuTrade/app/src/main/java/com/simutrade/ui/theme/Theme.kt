package com.simutrade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue600 = Color(0xFF2563EB)
private val Purple600 = Color(0xFF9333EA)
private val Green600 = Color(0xFF16A34A)
private val Red600 = Color(0xFFDC2626)

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    secondary = Purple600,
    tertiary = Green600,
    error = Red600,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onError = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue600,
    secondary = Purple600,
    tertiary = Green600,
    error = Red600,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onError = Color.White,
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1),
)

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