package com.burakbaybas.mantartanima.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// FungiLens logosundaki renkler
private val LogoGreen = Color(0xFF1B5E2E)
private val LogoBrown = Color(0xFF8A3B16)
private val LeafGreen = Color(0xFF5E9E45)
private val Cream = Color(0xFFFDF8F3)
private val Moss = Color(0xFF7FA36B)

private val LightColors = lightColorScheme(
    primary = LogoGreen,
    secondary = LogoBrown,
    tertiary = LeafGreen,
    background = Cream,
    surface = Cream,
    surfaceVariant = Color(0xFFE9F1E2),
    onSurface = Color(0xFF1E211E),
    onSurfaceVariant = Color(0xFF3F4A3F),
    primaryContainer = Color(0xFFDCEBD3),
    secondaryContainer = Color(0xFFE6F0DC),
)

private val DarkColors = darkColorScheme(
    primary = Moss,
    secondary = Color(0xFFD9B38C),
    tertiary = Color(0xFFA8C99A),
)

@Composable
fun MantarTanimaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        // Uygulama, telefon karanlık modda olsa da her zaman açık (beyaz-yeşil) temada çalışır
        colorScheme = LightColors,
        content = content,
    )
}
