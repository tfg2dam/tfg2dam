package com.simutrade.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores base de la paleta
private val Blue600   = Color(0xFF2563EB)
private val Blue100   = Color(0xFFDBEAFE)
private val Purple600 = Color(0xFF9333EA)
private val Green600  = Color(0xFF16A34A)
private val Red600    = Color(0xFFDC2626)

// Colores de los rangos de usuario
val ColorBronce   = Color(0xFFCD7F32)
val ColorPlata    = Color(0xFFC0C0C0)
val ColorOro      = Color(0xFFFFD700)
val ColorPlatino  = Color(0xFF9E9E9E)
val ColorDiamante = Color(0xFF00E5FF)

// Color para valores positivos (ganancias)
@Suppress("UnusedReceiverParameter")
val ColorScheme.positive: Color
    get() = Green600

// Color de fondo para valores positivos según el tema
@Suppress("UnusedReceiverParameter")
val ColorScheme.positiveContainer: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF166534) else Color(0xFFD1FAE5)

// Esquema de colores para tema claro
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
    onSurfaceVariant = Color(0xFF475569)
)

// Esquema de colores para tema oscuro
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
    onSurfaceVariant = Color(0xFFCBD5F5)
)

// Tema principal de la app, cambia automáticamente según el sistema
@Composable
fun SimuTradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content
    )
}